package niaromi.ui.browse.source

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import niaromi.core.navigation.LocalNavigator
import niaromi.presentation.browse.SourcesFilterScreen
import niaromi.presentation.core.screens.LoadingScreen
import niaromi.util.system.toast
import tachiyomi.i18n.MR

@Composable
fun SourcesFilterScreen() {
    val navigator = LocalNavigator.current
    val viewModel = metroViewModel<SourcesFilterViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state is SourcesFilterViewModel.State.Loading) {
        LoadingScreen()
        return
    }

    if (state is SourcesFilterViewModel.State.Error) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.toast(MR.strings.internal_error)
            navigator.pop()
        }
        return
    }

    val successState = state as SourcesFilterViewModel.State.Success

    SourcesFilterScreen(
        navigateUp = navigator::pop,
        state = successState,
        onClickLanguage = viewModel::toggleLanguage,
        onClickSource = viewModel::toggleSource,
    )
}
