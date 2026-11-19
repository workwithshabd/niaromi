package eu.kanade.presentation.more.settings.screen.data

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.backup.google.GoogleDriveSyncHelper
import eu.kanade.tachiyomi.data.backup.google.models.GoogleDriveFile
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Instant

@Composable
fun GoogleDriveBackupsDialog(
    syncHelper: GoogleDriveSyncHelper,
    onSelectBackup: (GoogleDriveFile) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var backups by remember { mutableStateOf<List<GoogleDriveFile>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            backups = syncHelper.listBackups()
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to list backups"
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.google_drive_restore_select_backup))
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 350.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isLoading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(MR.strings.google_drive_fetching_backups),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    backups.isEmpty() -> {
                        Text(
                            text = stringResource(MR.strings.google_drive_no_backups_found),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(backups) { file ->
                                val millis = remember(file.modifiedTime) {
                                    file.modifiedTime?.let {
                                        try {
                                            Instant.parse(it).toEpochMilliseconds()
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                                }
                                val dateStr = if (millis != null) {
                                    relativeTimeSpanString(millis)
                                } else {
                                    file.modifiedTime.orEmpty()
                                }
                                val sizeStr = remember(file.size) {
                                    file.size?.let { Formatter.formatFileSize(context, it) } ?: ""
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectBackup(file) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                ) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    val subText = listOf(dateStr, sizeStr)
                                        .filter { it.isNotEmpty() }
                                        .joinToString(" • ")
                                    if (subText.isNotEmpty()) {
                                        Text(
                                            text = subText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}
