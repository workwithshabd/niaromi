package niaromi.data.backup.google

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import logcat.LogPriority
import niaromi.core.common.util.system.logcat
import niaromi.data.backup.google.models.GoogleDriveFile
import niaromi.data.backup.google.models.GoogleDriveFileList
import niaromi.domain.backup.service.BackupPreferences
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

@Inject
@SingleIn(AppScope::class)
class GoogleDriveService(
    private val context: Context,
    private val networkHelper: NetworkHelper,
    private val json: Json,
    private val backupPreferences: BackupPreferences,
) {

    private val client get() = networkHelper.client

    suspend fun listBackups(): List<GoogleDriveFile> = withContext(Dispatchers.IO) {
        val token = GoogleSignInHelper.getAccessToken(context)
            ?: throw IOException("Google Account not signed in or token unavailable")

        val url = "https://www.googleapis.com/drive/v3/files" +
            "?spaces=appDataFolder,drive" +
            "&q=name+contains+'tachibk'+and+trashed=false" +
            "&fields=files(id,name,size,modifiedTime)" +
            "&orderBy=modifiedTime+desc"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        var response = client.newCall(request).execute()
        if (response.code == 401) {
            response.close()
            val freshToken = GoogleSignInHelper.clearAndRefreshToken(context, token)
                ?: throw IOException("Failed to refresh Google token")
            val retryRequest = request.newBuilder()
                .header("Authorization", "Bearer $freshToken")
                .build()
            response = client.newCall(retryRequest).execute()
        }

        response.use {
            if (!it.isSuccessful) {
                throw IOException("Google Drive list failed: code ${it.code}")
            }
            val body = it.body.string()
            val result = json.decodeFromString<GoogleDriveFileList>(body)
            result.files
        }
    }

    suspend fun uploadBackup(file: File, fileName: String): GoogleDriveFile = withContext(Dispatchers.IO) {
        val token = GoogleSignInHelper.getAccessToken(context)
            ?: throw IOException("Google Account not signed in or token unavailable")

        val metadataJson = """{"name":"$fileName","parents":["appDataFolder"]}"""
        val multipartBody = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(file.asRequestBody("application/octet-stream".toMediaType()))
            .build()

        val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(multipartBody)
            .build()

        var response = client.newCall(request).execute()
        if (response.code == 401) {
            response.close()
            val freshToken = GoogleSignInHelper.clearAndRefreshToken(context, token)
                ?: throw IOException("Failed to refresh Google token")
            val retryRequest = request.newBuilder()
                .header("Authorization", "Bearer $freshToken")
                .build()
            response = client.newCall(retryRequest).execute()
        }

        val uploadedFile = response.use {
            if (!it.isSuccessful) {
                throw IOException("Google Drive upload failed: code ${it.code}")
            }
            val body = it.body.string()
            json.decodeFromString<GoogleDriveFile>(body)
        }

        backupPreferences.lastGoogleDriveSyncTimestamp.set(System.currentTimeMillis())

        // Prune old backups, keeping up to MAX_KEEP_BACKUPS
        try {
            pruneOldBackups()
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Failed to prune old Google Drive backups" }
        }

        uploadedFile
    }

    suspend fun downloadBackup(fileId: String, destFile: File): File = withContext(Dispatchers.IO) {
        val token = GoogleSignInHelper.getAccessToken(context)
            ?: throw IOException("Google Account not signed in or token unavailable")

        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept-Encoding", "identity")
            .get()
            .build()

        var response = client.newCall(request).execute()
        if (response.code == 401) {
            response.close()
            val freshToken = GoogleSignInHelper.clearAndRefreshToken(context, token)
                ?: throw IOException("Failed to refresh Google token")
            val retryRequest = request.newBuilder()
                .header("Authorization", "Bearer $freshToken")
                .header("Accept-Encoding", "identity")
                .build()
            response = client.newCall(retryRequest).execute()
        }

        destFile.parentFile?.mkdirs()
        val tempFile = File(destFile.parentFile, "${destFile.name}.${System.currentTimeMillis()}.tmp")

        try {
            response.use { resp ->
                if (!resp.isSuccessful) {
                    throw IOException("Google Drive download failed: code ${resp.code}")
                }
                val body = resp.body
                tempFile.outputStream().use { fileOut ->
                    body.byteStream().use { bodyStream ->
                        bodyStream.copyTo(fileOut)
                    }
                    fileOut.fd.sync()
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                throw IOException("Downloaded backup file is empty or missing")
            }

            if (destFile.exists()) {
                destFile.delete()
            }
            if (!tempFile.renameTo(destFile)) {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            throw e
        }

        destFile
    }

    suspend fun deleteBackup(fileId: String) = withContext(Dispatchers.IO) {
        val token = GoogleSignInHelper.getAccessToken(context)
            ?: throw IOException("Google Account not signed in or token unavailable")

        val url = "https://www.googleapis.com/drive/v3/files/$fileId"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        response.use {
            if (!it.isSuccessful && it.code != 404) {
                throw IOException("Google Drive delete failed: code ${it.code}")
            }
        }
    }

    suspend fun pruneOldBackups(maxKeep: Int = MAX_KEEP_BACKUPS) {
        val backups = listBackups()
        if (backups.size > maxKeep) {
            val toDelete = backups.drop(maxKeep)
            for (file in toDelete) {
                try {
                    deleteBackup(file.id)
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) { "Failed to delete old backup ${file.id}" }
                }
            }
        }
    }

    companion object {
        private const val MAX_KEEP_BACKUPS = 2
    }
}
