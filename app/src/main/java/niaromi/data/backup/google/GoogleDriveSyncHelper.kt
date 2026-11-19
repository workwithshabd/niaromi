package niaromi.data.backup.google

import android.content.Context
import android.net.Uri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import niaromi.core.common.util.system.logcat
import niaromi.data.backup.create.BackupCreator
import niaromi.data.backup.create.BackupOptions
import niaromi.data.backup.google.models.GoogleDriveFile
import niaromi.data.backup.restore.BackupRestoreWorker
import niaromi.data.backup.restore.RestoreOptions
import niaromi.domain.backup.service.BackupPreferences
import niaromi.util.storage.getUriCompat
import niaromi.util.system.workManager
import java.io.File

@Inject
@SingleIn(AppScope::class)
class GoogleDriveSyncHelper(
    private val context: Context,
    private val googleDriveService: GoogleDriveService,
    private val backupCreatorFactory: BackupCreator.Factory,
    private val backupPreferences: BackupPreferences,
) {

    private var lastAutoRestoreCheck = 0L

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
                val uploaded = googleDriveService.uploadBackup(tempFile, fileName)
                backupPreferences.lastRestoredGoogleDriveBackupId.set(uploaded.id)
                uploaded
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

    suspend fun pruneOldBackups(maxKeep: Int = 2) {
        googleDriveService.pruneOldBackups(maxKeep)
    }

    suspend fun autoRestoreLatestBackup(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!GoogleSignInHelper.isSignedIn(context)) {
            return@withContext false
        }
        if (!force && !backupPreferences.googleDriveAutoRestore.get()) {
            return@withContext false
        }
        val now = System.currentTimeMillis()
        if (!force && now - lastAutoRestoreCheck < 5 * 60 * 1000L) {
            return@withContext false
        }
        lastAutoRestoreCheck = now

        if (BackupRestoreWorker.isRunning(context.workManager)) {
            logcat(LogPriority.INFO) { "Backup restore already in progress, skipping auto restore" }
            return@withContext false
        }

        try {
            val backups = googleDriveService.listBackups()
            if (backups.isEmpty()) {
                return@withContext false
            }
            val latest = backups.first()
            val lastRestoredId = backupPreferences.lastRestoredGoogleDriveBackupId.get()
            if (!force && latest.id == lastRestoredId) {
                logcat(LogPriority.INFO) { "Google Drive backup ${latest.id} already restored, skipping" }
                return@withContext false
            }

            logcat(LogPriority.INFO) { "Auto-restoring latest Google Drive backup: ${latest.name} (${latest.id})" }
            val downloadedFile = downloadBackup(latest.id, latest.name)
            val uri = downloadedFile.getUriCompat(context)
            backupPreferences.lastRestoredGoogleDriveBackupId.set(latest.id)
            BackupRestoreWorker.start(
                workManager = context.workManager,
                uri = uri,
                options = RestoreOptions(),
                sync = true,
            )
            try {
                pruneOldBackups(2)
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Failed to prune old Google Drive backups during auto restore" }
            }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to auto-restore backup from Google Drive" }
            false
        }
    }
}
