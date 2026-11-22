package niaromi.presentation.crash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.PreviewLightDark
import kotlinx.coroutines.launch
import niaromi.app.di.appGraph
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.rounded.BugReport
import niaromi.presentation.core.components.material.padding
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.core.screens.InfoScreen
import niaromi.presentation.theme.TachiyomiPreviewTheme
import niaromi.util.system.copyToClipboard
import tachiyomi.i18n.MR

@Composable
fun CrashScreen(
    exception: Throwable?,
    onRestartClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val crashLogUtil = remember { context.appGraph.crashLogUtil }

    InfoScreen(
        icon = MaterialSymbols.Rounded.BugReport,
        headingText = stringResource(MR.strings.crash_screen_title),
        subtitleText = stringResource(MR.strings.crash_screen_description, stringResource(MR.strings.app_name)),
        acceptText = "Share crash log",
        onAcceptClick = {
            scope.launch {
                crashLogUtil.dumpLogs(exception)
            }
        },
        rejectText = stringResource(MR.strings.crash_screen_restart_application),
        onRejectClick = onRestartClick,
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = MaterialTheme.padding.small)) {
            Text(
                "To report this crash, tag @workwithshabd on X or mention workwithshabd on Discord. Review your crash log before sharing it.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row {
                TextButton(onClick = { uriHandler.openUri("https://x.com/workwithshabd") }) {
                    Text("Open X")
                }
                TextButton(onClick = { context.copyToClipboard("Discord username", "workwithshabd") }) {
                    Text("Copy Discord username")
                }
            }
            if (exception != null) {
                Text(
                    text = exception.toString(),
                    modifier = Modifier.fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(MaterialTheme.padding.medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 5,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun CrashScreenPreview() {
    TachiyomiPreviewTheme {
        CrashScreen(exception = RuntimeException("Dummy")) {}
    }
}
