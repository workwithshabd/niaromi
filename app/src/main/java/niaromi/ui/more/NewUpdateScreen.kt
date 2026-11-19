package niaromi.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import niaromi.core.navigation.LocalNavigator
import niaromi.presentation.more.NewUpdateScreen
import niaromi.util.system.openInBrowser

@Composable
fun NewUpdateScreen(
    versionName: String,
    changelogInfo: String,
    releaseLink: String,
    downloadLink: String,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val viewModel =
        assistedMetroViewModel<NewUpdateScreenModel, NewUpdateScreenModel.Factory> {
            create(changelogInfo = changelogInfo, downloadLink = downloadLink)
        }

    val state by viewModel.state.collectAsState()

    NewUpdateScreen(
        versionName = versionName,
        stage = state.stage,
        downloadProgress = { state.downloadProgress },
        changelogInfo = state.changelogInfo,
        onOpenInBrowser = { context.openInBrowser(releaseLink) },
        onAcceptUpdate = {
            when (state.stage) {
                NewUpdateScreenModel.Stage.Available, NewUpdateScreenModel.Stage.Failed -> viewModel.startDownload()
                NewUpdateScreenModel.Stage.Downloaded -> viewModel.installUpdate()
                else -> Unit
            }
        },
        onRejectUpdate = navigator::pop,
        onSkipUpdate = {
            viewModel.skipUpdate(versionName)
            navigator.pop()
        },
    )
}
