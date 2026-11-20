package niaromi.presentation.more.onboarding

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import logcat.LogPriority
import niaromi.app.di.appGraph
import niaromi.core.common.i18n.stringResource
import niaromi.core.common.util.system.logcat
import niaromi.data.backup.google.GoogleSignInHelper
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.rounded.Folder
import niaromi.icons.materialsymbols.rounded.Sync
import niaromi.presentation.core.components.material.Button
import niaromi.util.system.toast
import tachiyomi.i18n.MR
import niaromi.presentation.core.i18n.stringResource as composeStringResource

internal class RestoreStep(
    private val onRestoreLocal: (String) -> Unit,
) : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val backupPreferences = remember { context.appGraph.backupPreferences }
        val syncHelper = remember { context.appGraph.googleDriveSyncHelper }

        var googleAccount by remember {
            mutableStateOf(GoogleSignInHelper.getSignedInAccount(context))
        }
        var isSyncingGoogle by remember { mutableStateOf(false) }

        val chooseBackupLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (e: SecurityException) {
                    logcat(LogPriority.ERROR, e)
                }
                onRestoreLocal(uri.toString())
            }
        }

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
                isSyncingGoogle = true
                scope.launch {
                    try {
                        val restored = syncHelper.autoRestoreLatestBackup(force = true)
                        if (restored) {
                            context.toast("Restoring latest backup from Google Drive...")
                        } else {
                            context.toast("No existing backup found on Google Drive.")
                        }
                    } finally {
                        isSyncingGoogle = false
                    }
                }
            } catch (e: ApiException) {
                logcat(LogPriority.ERROR, e) { "Google Sign-In failed" }
                context.toast(MR.strings.google_drive_sign_in_failed)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Restore Backup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Have an existing library? Restore your manga, history, and categories " +
                    "from local storage or sign in with Google Drive.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Card 1: Restore from Local File
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Column {
                            Text(
                                text = "Local Backup File",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Restore from a .tachibk backup file on device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            try {
                                chooseBackupLauncher.launch(arrayOf("*/*"))
                            } catch (e: Exception) {
                                context.toast(MR.strings.file_picker_error)
                            }
                        },
                    ) {
                        Text("Select Backup File")
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 4.dp),
            )

            // Card 2: Google Drive Sign-In / Restore
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Column {
                            Text(
                                text = "Google Drive Sync",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (googleAccount != null) {
                                    "Connected as ${googleAccount?.email}"
                                } else {
                                    "Sign in to automatically sync and restore library"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (googleAccount != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = !isSyncingGoogle,
                                onClick = {
                                    isSyncingGoogle = true
                                    scope.launch {
                                        try {
                                            val restored = syncHelper.autoRestoreLatestBackup(force = true)
                                            if (restored) {
                                                context.toast("Restoring latest backup...")
                                            } else {
                                                context.toast("No backups found on Google Drive.")
                                            }
                                        } finally {
                                            isSyncingGoogle = false
                                        }
                                    }
                                },
                            ) {
                                if (isSyncingGoogle) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Restore from Drive")
                            }

                            OutlinedButton(
                                onClick = {
                                    GoogleSignInHelper.signOut(context) {
                                        googleAccount = null
                                        backupPreferences.googleDriveSync.set(false)
                                    }
                                },
                            ) {
                                Text("Sign Out")
                            }
                        }
                    } else {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                try {
                                    signInLauncher.launch(GoogleSignInHelper.getSignInClient(context).signInIntent)
                                } catch (e: Exception) {
                                    context.toast(MR.strings.google_drive_sign_in_failed)
                                }
                            },
                        ) {
                            Text("Sign in with Google")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Or tap Finish below if this is a fresh setup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
