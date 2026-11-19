package niaromi.ui.browse.source

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import niaromi.core.navigation.BrowseSourceRoute
import niaromi.core.navigation.GlobalSearchRoute
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.SourcesFilterRoute
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.rounded.FilterList
import niaromi.icons.materialsymbols.rounded.TravelExplore
import niaromi.presentation.browse.SourceOptionsDialog
import niaromi.presentation.browse.SourcesScreen
import niaromi.presentation.components.AppBar
import niaromi.presentation.components.TabContent
import niaromi.presentation.core.i18n.stringResource
import tachiyomi.i18n.MR

@Composable
fun sourcesTab(): TabContent {
    val navigator = LocalNavigator.current
    val viewModel = metroViewModel<SourcesViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    return TabContent(
        titleRes = MR.strings.label_sources,
        actions = listOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_global_search),
                icon = MaterialSymbols.Rounded.TravelExplore,
                onClick = { navigator.push(GlobalSearchRoute()) },
            ),
            AppBar.Action(
                title = stringResource(MR.strings.action_filter),
                icon = MaterialSymbols.Rounded.FilterList,
                onClick = { navigator.push(SourcesFilterRoute) },
            ),
        ),
        content = { contentPadding, snackbarHostState ->
            SourcesScreen(
                state = state,
                contentPadding = contentPadding,
                onClickItem = { source, listing ->
                    navigator.push(BrowseSourceRoute(source.id, listing.query))
                },
                onClickPin = viewModel::togglePin,
                onLongClickItem = viewModel::showSourceDialog,
            )

            state.dialog?.let { dialog ->
                val source = dialog.source
                SourceOptionsDialog(
                    source = source,
                    onClickPin = {
                        viewModel.togglePin(source)
                        viewModel.closeDialog()
                    },
                    onClickDisable = {
                        viewModel.toggleSource(source)
                        viewModel.closeDialog()
                    },
                    onDismiss = viewModel::closeDialog,
                )
            }

            val internalErrString = stringResource(MR.strings.internal_error)
            LaunchedEffect(Unit) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        SourcesViewModel.Event.FailedFetchingSources -> {
                            launch { snackbarHostState.showSnackbar(internalErrString) }
                        }
                    }
                }
            }
        },
    )
}
