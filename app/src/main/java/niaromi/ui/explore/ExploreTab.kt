package niaromi.ui.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.MangaRoute
import niaromi.presentation.explore.ExploreScreen

@Composable
fun ExploreTab() {
    val navigator = LocalNavigator.current
    val viewModel = metroViewModel<ExploreViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    ExploreScreen(
        state = state,
        onMangaClick = { manga ->
            navigator.push(MangaRoute(manga.id))
        },
        onRefresh = viewModel::refresh,
        onSearchQueryChange = viewModel::setSearchQuery,
        onSelectSourceFilter = viewModel::selectSourceFilter,
    )
}
