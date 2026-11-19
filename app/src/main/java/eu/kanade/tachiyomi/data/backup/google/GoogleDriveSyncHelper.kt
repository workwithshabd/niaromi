package eu.kanade.tachiyomi.data.backup.google

import android.content.Context
import android.net.Uri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.google.models.GoogleDriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.service.BackupPreferences
import java.io.File

@Inject
@SingleIn(AppScope::class)
class GoogleDriveSyncHelper(
    private val context: Context,
    private val googleDriveService: GoogleDriveService,
    private val backupCreatorFactory: BackupCreator.Factory,
    private val backupPreferences: BackupPreferences,
) {

    suspend fun createAndUploadBackup(options: BackupOptions = BackupOptions()): GoogleDriveFile =
        withContext(Dispatchers.IO) {
            val fileName = BackupCreator.getFilename()
            val tempFile = File(context.cacheDir, fileName)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            tempFile.createNewFile()

            try {
                val fileUri = Uri.fromFile(tempFile)
                backupCreatorFactory.create(isAutoBackup = false).backup(fileUri, options)
                googleDriveService.uploadBackup(tempFile, fileName)
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }

    suspend fun downloadBackup(fileId: String, fileName: String): File = withContext(Dispatchers.IO) {
        val destFile = File(context.cacheDir, fileName)
        if (destFile.exists()) {
            destFile.delete()
        }
        destFile.createNewFile()
        googleDriveService.downloadBackup(fileId, destFile)
    }

    suspend fun listBackups(): List<GoogleDriveFile> {
        return googleDriveService.listBackups()
    }
}
