package niaromi.ui.browse.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest
import niaromi.core.common.i18n.stringResource
import niaromi.core.navigation.LocalNavigator
import niaromi.presentation.browse.ExtensionFilterScreen
import niaromi.presentation.core.screens.LoadingScreen
import tachiyomi.i18n.MR

@Composable
fun ExtensionFilterScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val viewModel = metroViewModel<ExtensionFilterViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state is ExtensionFilterState.Loading) {
        LoadingScreen()
        return
    }

    val successState = state as ExtensionFilterState.Success

    ExtensionFilterScreen(
        navigateUp = navigator::pop,
        state = successState,
        onClickToggle = viewModel::toggle,
    )

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest {
            when (it) {
                ExtensionFilterEvent.FailedFetchingLanguages -> {
                    context.stringResource(MR.strings.internal_error)
                }
            }
        }
    }
}
