package niaromi.ui.explore

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.MangaRoute
import niaromi.presentation.explore.ExploreScreen
import niaromi.ui.home.TopLevelRoute

@Composable
fun ExploreTab() {
    val navigator = LocalNavigator.current
    val viewModel = metroViewModel<ExploreViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    ResultEffect<Unit>(resultKey = TopLevelRoute.Explore.reselectEventKey) {
        scope.launch { listState.animateScrollToItem(0) }
    }

    ExploreScreen(
        state = state,
        listState = listState,
        onMangaClick = { manga ->
            navigator.push(MangaRoute(manga.id, fromSearch = true))
        },
        onRefresh = viewModel::refresh,
        onSearchQueryChange = viewModel::setSearchQuery,
        onSelectSourceFilter = viewModel::selectSourceFilter,
        onLoadMore = viewModel::loadNextPage,
    )
}
