package niaromi.domain.backup.service

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import niaromi.core.common.preference.Preference
import niaromi.core.common.preference.PreferenceStore

@Inject
@SingleIn(AppScope::class)
class BackupPreferences(
    preferenceStore: PreferenceStore,
) {

    val backupInterval: Preference<Int> = preferenceStore.getInt("backup_interval", 12)

    val lastAutoBackupTimestamp: Preference<Long> = preferenceStore.getLong(
        Preference.appStateKey("last_auto_backup_timestamp"),
        0L,
    )

    val googleDriveSync: Preference<Boolean> = preferenceStore.getBoolean("google_drive_sync_enabled", false)

    val googleDriveAccountEmail: Preference<String> = preferenceStore.getString("google_drive_account_email", "")

    val googleDriveAccountName: Preference<String> = preferenceStore.getString("google_drive_account_name", "")

    val googleDriveAccountPhoto: Preference<String> = preferenceStore.getString("google_drive_account_photo", "")

    val lastGoogleDriveSyncTimestamp: Preference<Long> = preferenceStore.getLong(
        Preference.appStateKey("last_google_drive_sync_timestamp"),
        0L,
    )

    val googleDriveAutoSync: Preference<Boolean> = preferenceStore.getBoolean("google_drive_auto_sync", true)

    val googleDriveAutoRestore: Preference<Boolean> = preferenceStore.getBoolean("google_drive_auto_restore", true)

    val lastRestoredGoogleDriveBackupId: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("last_restored_google_drive_backup_id"),
        "",
    )
}
