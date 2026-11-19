package eu.kanade.presentation.more.settings.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.net.toUri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.hippo.unifile.UniFile
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.data.GoogleDriveBackupsDialog
import eu.kanade.presentation.more.settings.screen.data.StorageInfo
import eu.kanade.presentation.more.settings.widget.BasePreferenceWidget
import eu.kanade.presentation.more.settings.widget.PrefsHorizontalPadding
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.backup.create.BackupCreateWorker
import eu.kanade.tachiyomi.data.backup.google.GoogleSignInHelper
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreWorker
import eu.kanade.tachiyomi.data.export.LibraryExporter
import eu.kanade.tachiyomi.data.export.LibraryExporter.ExportOptions
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import logcat.LogPriority
import niaromi.app.di.appGraph
import niaromi.core.navigation.CreateBackupRoute
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.RestoreBackupRoute
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.automirroredrounded.Help
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.displayablePath
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Serializable
data object SettingsDataScreen : SearchableSettings {

    val restorePreferenceKeyString = MR.strings.label_backup

    const val HELP_URL = "https://github.com/workwithshabd/niaromi"

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.label_data_storage

    @Composable
    override fun RowScope.AppBarAction() {
        val uriHandler = LocalUriHandler.current
        IconButton(onClick = { uriHandler.openUri(HELP_URL) }) {
            Icon(
                imageVector = MaterialSymbols.AutoMirroredRounded.Help,
                contentDescription = stringResource(MR.strings.tracking_guide),
            )
        }
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val backupPreferences = remember { context.appGraph.backupPreferences }
        val storagePreferences = remember { context.appGraph.storagePreferences }

        return listOf(
            getStorageLocationPref(storagePreferences = storagePreferences),
            Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.pref_storage_location_info)),

            getEbookGroup(),
            getBackupAndRestoreGroup(backupPreferences = backupPreferences),
            getGoogleDriveGroup(backupPreferences = backupPreferences),
            getDataGroup(),
            getExportGroup(),
        )
    }

    @Composable
    fun storageLocationPicker(
        storageDirPref: tachiyomi.core.common.preference.Preference<String>,
    ): ManagedActivityResultLauncher<Uri?, Uri?> {
        val context = LocalContext.current

        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri != null) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                // For some reason InkBook devices do not implement the SAF properly. Persistable URI grants do not
                // work. However, simply retrieving the URI and using it works fine for these devices. Access is not
                // revoked after the app is closed or the device is restarted.
                // This also holds for some Samsung devices. Thus, we simply execute inside of a try-catch block and
                // ignore the exception if it is thrown.
                try {
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (e: SecurityException) {
                    logcat(LogPriority.ERROR, e)
                    context.toast(MR.strings.file_picker_uri_permission_unsupported)
                }

                UniFile.fromUri(context, uri)?.let {
                    storageDirPref.set(it.uri.toString())
                }
            }
        }
    }

    @Composable
    fun storageLocationText(
        storageDirPref: tachiyomi.core.common.preference.Preference<String>,
    ): String {
        val context = LocalContext.current
        val storageDir by storageDirPref.collectAsState()

        if (storageDir == storageDirPref.defaultValue()) {
            return stringResource(MR.strings.no_location_set)
        }

        return remember(storageDir) {
            val file = UniFile.fromUri(context, storageDir.toUri())
            file?.displayablePath
        } ?: stringResource(MR.strings.invalid_location, storageDir)
    }

    @Composable
    private fun getStorageLocationPref(
        storagePreferences: StoragePreferences,
    ): Preference.PreferenceItem.TextPreference {
        val context = LocalContext.current
        val pickStorageLocation = storageLocationPicker(storagePreferences.baseStorageDirectory)

        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.pref_storage_location),
            subtitle = storageLocationText(storagePreferences.baseStorageDirectory),
            onClick = {
                try {
                    pickStorageLocation.launch(null)
                } catch (e: ActivityNotFoundException) {
                    context.toast(MR.strings.file_picker_error)
                }
            },
        )
    }

    @Composable
    private fun getBackupAndRestoreGroup(backupPreferences: BackupPreferences): Preference.PreferenceGroup {
        val context = LocalContext.current
        val navigator = LocalNavigator.current
        val scope = rememberCoroutineScope()

        val lastAutoBackup by backupPreferences.lastAutoBackupTimestamp.collectAsState()

        val chooseBackup = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) {
            if (it == null) {
                context.toast(MR.strings.file_null_uri_error)
                return@rememberLauncherForActivityResult
            }

            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (e: SecurityException) {
                logcat(LogPriority.ERROR, e)
            }

            navigator.push(RestoreBackupRoute(it.toString()))
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_backup),
            preferenceItems = listOf(
                // Manual actions
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(restorePreferenceKeyString),
                ) {
                    BasePreferenceWidget(
                        subcomponent = {
                            MultiChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(intrinsicSize = IntrinsicSize.Min)
                                    .padding(horizontal = PrefsHorizontalPadding),
                            ) {
                                SegmentedButton(
                                    modifier = Modifier.fillMaxHeight(),
                                    checked = false,
                                    onCheckedChange = { navigator.push(CreateBackupRoute) },
                                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                                ) {
                                    Text(stringResource(MR.strings.pref_create_backup))
                                }
                                SegmentedButton(
                                    modifier = Modifier.fillMaxHeight(),
                                    checked = false,
                                    onCheckedChange = {
                                        scope.launch {
                                            if (!BackupRestoreWorker.isRunning(context.workManager)) {
                                                if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                                                    context.toast(MR.strings.restore_miui_warning)
                                                }
                                                chooseBackup.launch(arrayOf("*/*"))
                                            } else {
                                                context.toast(MR.strings.restore_in_progress)
                                            }
                                        }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                                ) {
                                    Text(stringResource(MR.strings.pref_restore_backup))
                                }
                            }
                        },
                    )
                },

                // Automatic backups
                Preference.PreferenceItem.ListPreference(
                    preference = backupPreferences.backupInterval,
                    entries = mapOf(
                        0 to stringResource(MR.strings.off),
                        6 to stringResource(MR.strings.update_6hour),
                        12 to stringResource(MR.strings.update_12hour),
                        24 to stringResource(MR.strings.update_24hour),
                        48 to stringResource(MR.strings.update_48hour),
                        168 to stringResource(MR.strings.update_weekly),
                    ),
                    title = stringResource(MR.strings.pref_backup_interval),
                    onValueChanged = {
                        BackupCreateWorker.setupTask(context, it)
                        true
                    },
                ),
                Preference.PreferenceItem.InfoPreference(
                    stringResource(MR.strings.backup_info) + "\n\n" +
                        stringResource(MR.strings.last_auto_backup_info, relativeTimeSpanString(lastAutoBackup)),
                ),
            ),
        )
    }

    @Composable
    private fun getGoogleDriveGroup(backupPreferences: BackupPreferences): Preference.PreferenceGroup {
        val context = LocalContext.current
        val navigator = LocalNavigator.current
        val scope = rememberCoroutineScope()
        val syncHelper = remember { context.appGraph.googleDriveSyncHelper }

        var googleAccount by remember {
            mutableStateOf(GoogleSignInHelper.getSignedInAccount(context))
        }
        val lastSyncTimestamp by backupPreferences.lastGoogleDriveSyncTimestamp.collectAsState()
        var showBackupsDialog by remember { mutableStateOf(false) }

        val signInLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                googleAccount = GoogleSignInHelper.getSignedInAccount(context)
                backupPreferences.googleDriveSync.set(true)
                account.email?.let { backupPreferences.googleDriveAccountEmail.set(it) }
                account.displayName?.let { backupPreferences.googleDriveAccountName.set(it) }
                account.photoUrl?.toString()?.let { backupPreferences.googleDriveAccountPhoto.set(it) }
                context.toast(context.stringResource(MR.strings.google_drive_signed_in_as, account.email ?: ""))
                scope.launch {
                    syncHelper.autoRestoreLatestBackup()
                }
            } catch (e: ApiException) {
                logcat(LogPriority.ERROR, e) { "Google Sign-In failed with status ${e.statusCode}" }
                val errorMsg = when (e.statusCode) {
                    com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                        "Sign-in cancelled. Tap 'Continue' on the Google prompt to grant permission."
                    com.google.android.gms.common.api.CommonStatusCodes.DEVELOPER_ERROR ->
                        "Developer error (10): OAuth client SHA-1 or package name mismatch."
                    else -> context.stringResource(MR.strings.google_drive_sign_in_failed) + " (${e.statusCode})"
                }
                context.toast(errorMsg)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Google Sign-In failed" }
                context.toast(MR.strings.google_drive_sign_in_failed)
            }
        }

        if (showBackupsDialog) {
            GoogleDriveBackupsDialog(
                syncHelper = syncHelper,
                onSelectBackup = { file ->
                    showBackupsDialog = false
                    scope.launch {
                        try {
                            context.toast(MR.strings.google_drive_downloading)
                            val downloadedFile = syncHelper.downloadBackup(file.id, file.name)
                            val contentUri = downloadedFile.getUriCompat(context)
                            navigator.push(RestoreBackupRoute(contentUri.toString()))
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR, e) { "Failed to download backup from Google Drive" }
                            context.toast(context.stringResource(MR.strings.google_drive_sync_error, e.message ?: ""))
                        }
                    }
                },
                onDismissRequest = { showBackupsDialog = false },
            )
        }

        val items = mutableListOf<Preference.PreferenceItem<out Any, out Any>>()

        val currentAccount = googleAccount
        if (currentAccount == null) {
            items.add(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.google_drive_sign_in),
                    subtitle = stringResource(MR.strings.pref_google_drive_summary),
                    onClick = {
                        try {
                            signInLauncher.launch(GoogleSignInHelper.getSignInClient(context).signInIntent)
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR, e) { "Failed to launch Google Sign-In" }
                            context.toast(MR.strings.google_drive_sign_in_failed)
                        }
                    },
                ),
            )
        } else {
            items.add(
                Preference.PreferenceItem.TextPreference(
                    title = currentAccount.displayName?.ifEmpty { currentAccount.email } ?: currentAccount.email,
                    subtitle = stringResource(MR.strings.google_drive_signed_in_as, currentAccount.email),
                    onClick = {
                        GoogleSignInHelper.signOut(context) {
                            googleAccount = null
                            backupPreferences.googleDriveSync.set(false)
                            backupPreferences.googleDriveAccountEmail.set("")
                            backupPreferences.googleDriveAccountName.set("")
                            backupPreferences.googleDriveAccountPhoto.set("")
                        }
                    },
                ),
            )
            items.add(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.google_drive_backup_now),
                    subtitle = if (lastSyncTimestamp > 0L) {
                        stringResource(MR.strings.google_drive_last_synced, relativeTimeSpanString(lastSyncTimestamp))
                    } else {
                        stringResource(MR.strings.google_drive_never_synced)
                    },
                    onClick = {
                        scope.launch {
                            try {
                                context.toast(MR.strings.google_drive_uploading)
                                syncHelper.createAndUploadBackup()
                                context.toast(MR.strings.google_drive_sync_success)
                            } catch (e: Exception) {
                                logcat(LogPriority.ERROR, e) { "Google Drive backup failed" }
                                context.toast(
                                    context.stringResource(MR.strings.google_drive_sync_error, e.message ?: ""),
                                )
                            }
                        }
                    },
                ),
            )
            items.add(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.google_drive_restore_now),
                    subtitle = stringResource(MR.strings.google_drive_restore_select_backup),
                    onClick = {
                        showBackupsDialog = true
                    },
                ),
            )
            items.add(
                Preference.PreferenceItem.SwitchPreference(
                    preference = backupPreferences.googleDriveAutoSync,
                    title = stringResource(MR.strings.google_drive_auto_sync),
                    subtitle = stringResource(MR.strings.google_drive_auto_sync_summary),
                ),
            )
            items.add(
                Preference.PreferenceItem.SwitchPreference(
                    preference = backupPreferences.googleDriveAutoRestore,
                    title = stringResource(MR.strings.google_drive_auto_restore),
                    subtitle = stringResource(MR.strings.google_drive_auto_restore_summary),
                ),
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_google_drive),
            preferenceItems = items,
        )
    }

    @Composable
    private fun getEbookGroup(): Preference.PreferenceGroup {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val storageManager = remember { context.appGraph.storageManager }

        val chooseEpub = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris ->
            if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                var imported = 0
                val localDir = storageManager.getLocalSourceDirectory()
                if (localDir != null) {
                    uris.forEach { uri ->
                        try {
                            val filename =
                                UniFile.fromUri(context, uri)?.name ?: "book_${System.currentTimeMillis()}.epub"
                            val targetFile = localDir.createFile(filename)
                            if (targetFile != null) {
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    targetFile.openOutputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                imported++
                            }
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR, e) { "Failed to import ebook" }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    if (imported > 0) {
                        context.toast("Imported $imported eBook(s) to Local source")
                    } else {
                        context.toast("Failed to import eBook")
                    }
                }
            }
        }

        return Preference.PreferenceGroup(
            title = "eBooks & EPUBs",
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = "Import eBook (EPUB)",
                    subtitle = "Import .epub books and novels into your local collection",
                    onClick = {
                        try {
                            chooseEpub.launch(arrayOf("application/epub+zip", "application/octet-stream", "*/*"))
                        } catch (e: ActivityNotFoundException) {
                            context.toast(MR.strings.file_picker_error)
                        }
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getDataGroup(): Preference.PreferenceGroup {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val libraryPreferences = remember { context.appGraph.libraryPreferences }

        val chapterCache = remember { context.appGraph.chapterCache }
        var cacheReadableSizeSema by remember { mutableIntStateOf(0) }
        val cacheReadableSize by produceState("", cacheReadableSizeSema) {
            value = chapterCache.readableSize()
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_storage_usage),
            preferenceItems = listOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_storage_usage),
                ) {
                    BasePreferenceWidget(
                        subcomponent = {
                            StorageInfo(
                                modifier = Modifier.padding(horizontal = PrefsHorizontalPadding),
                            )
                        },
                    )
                },

                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_clear_chapter_cache),
                    subtitle = stringResource(MR.strings.used_cache, cacheReadableSize),
                    onClick = {
                        scope.launch {
                            try {
                                val deletedFiles = chapterCache.clear()
                                context.toast(context.stringResource(MR.strings.cache_deleted, deletedFiles))
                                cacheReadableSizeSema++
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                logcat(LogPriority.ERROR, e)
                                context.toast(MR.strings.cache_delete_error)
                            }
                        }
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.autoClearChapterCache,
                    title = stringResource(MR.strings.pref_auto_clear_chapter_cache),
                ),
            ),
        )
    }

    @Composable
    private fun getExportGroup(): Preference.PreferenceGroup {
        var showDialog by remember { mutableStateOf(false) }
        var exportOptions by remember {
            mutableStateOf(
                ExportOptions(
                    includeTitle = true,
                    includeAuthor = true,
                    includeArtist = true,
                ),
            )
        }

        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val getFavorites = remember { context.appGraph.getFavorites }
        var favorites by remember { mutableStateOf<List<Manga>>(emptyList()) }
        LaunchedEffect(Unit) {
            favorites = getFavorites.await()
        }

        val saveFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/csv"),
        ) { uri ->
            uri?.let {
                scope.launch {
                    LibraryExporter.exportToCsv(
                        context = context,
                        uri = it,
                        favorites = favorites,
                        options = exportOptions,
                        onExportComplete = {
                            scope.launch(Dispatchers.Main) {
                                context.toast(MR.strings.library_exported)
                            }
                        },
                    )
                }
            }
        }

        if (showDialog) {
            ColumnSelectionDialog(
                options = exportOptions,
                onConfirm = { options ->
                    exportOptions = options
                    saveFileLauncher.launch("niaromi_library.csv")
                },
                onDismissRequest = { showDialog = false },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.export),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.library_list),
                    onClick = { showDialog = true },
                ),
            ),
        )
    }

    @Composable
    private fun ColumnSelectionDialog(
        options: ExportOptions,
        onConfirm: (ExportOptions) -> Unit,
        onDismissRequest: () -> Unit,
    ) {
        var titleSelected by remember { mutableStateOf(options.includeTitle) }
        var authorSelected by remember { mutableStateOf(options.includeAuthor) }
        var artistSelected by remember { mutableStateOf(options.includeArtist) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(text = stringResource(MR.strings.migration_dialog_what_to_include))
            },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = titleSelected,
                            onCheckedChange = { checked ->
                                titleSelected = checked
                                if (!checked) {
                                    authorSelected = false
                                    artistSelected = false
                                }
                            },
                        )
                        Text(text = stringResource(MR.strings.title))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = authorSelected,
                            onCheckedChange = { authorSelected = it },
                            enabled = titleSelected,
                        )
                        Text(text = stringResource(MR.strings.author))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = artistSelected,
                            onCheckedChange = { artistSelected = it },
                            enabled = titleSelected,
                        )
                        Text(text = stringResource(MR.strings.artist))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(
                            ExportOptions(
                                includeTitle = titleSelected,
                                includeAuthor = authorSelected,
                                includeArtist = artistSelected,
                            ),
                        )
                        onDismissRequest()
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
        )
    }
}
