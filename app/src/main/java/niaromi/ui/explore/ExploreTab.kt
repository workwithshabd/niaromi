package niaromi.ui.explore

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.MangaRoute
import niaromi.presentation.explore.ExploreScreen
import niaromi.ui.browse.source.globalsearch.GlobalSearchViewModel
import niaromi.ui.home.TopLevelRoute

@Composable
fun ExploreTab() {
    val navigator = LocalNavigator.current
    val viewModel = metroViewModel<ExploreViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchViewModel = assistedMetroViewModel<GlobalSearchViewModel, GlobalSearchViewModel.Factory> {
        create(initialQuery = "", initialExtensionFilter = null, searchAllSources = true)
    }
    val searchState by searchViewModel.state.collectAsState()
    val feedListState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val query = state.searchQuery.trim()
    val listState = if (query.isEmpty()) feedListState else searchListState
    val search = remember(searchViewModel) {
        { text: String ->
            searchViewModel.updateSearchQuery(text.trim())
            searchViewModel.search()
        }
    }
    LaunchedEffect(viewModel) { viewModel.refreshIfStale() }
    LaunchedEffect(query) {
        if (query.isNotEmpty()) {
            searchListState.scrollToItem(0)
            delay(350)
        }
        search(query)
    }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    ResultEffect<Unit>(resultKey = TopLevelRoute.Explore.reselectEventKey) {
        focusManager.clearFocus()
        viewModel.setSearchQuery("")
        scope.launch { feedListState.scrollToItem(0) }
    }

    ExploreScreen(
        state = state,
        listState = listState,
        searchState = searchState,
        onMangaClick = { manga ->
            navigator.push(MangaRoute(manga.id, fromSource = query.isNotEmpty(), fromSearch = true))
        },
        onRefresh = viewModel::refresh,
        onSearchQueryChange = viewModel::setSearchQuery,
        onSearchExtensions = search,
        onLoadMore = viewModel::loadNextPage,
    )
}
