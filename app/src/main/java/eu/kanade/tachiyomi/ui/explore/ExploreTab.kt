package eu.kanade.tachiyomi.ui.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.explore.ExploreScreen
import mihon.core.navigation.LocalNavigator
import mihon.core.navigation.MangaRoute

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
