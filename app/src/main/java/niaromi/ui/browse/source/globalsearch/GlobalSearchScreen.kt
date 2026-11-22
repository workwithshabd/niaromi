package niaromi.ui.browse.source.globalsearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import niaromi.core.navigation.BrowseSourceRoute
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.MangaRoute
import niaromi.presentation.browse.GlobalSearchScreen
import niaromi.presentation.core.screens.LoadingScreen

@Composable
fun GlobalSearchScreen(
    searchQuery: String,
    extensionFilter: String?,
    searchAllSources: Boolean,
) {
    val navigator = LocalNavigator.current

    val viewModel =
        assistedMetroViewModel<GlobalSearchViewModel, GlobalSearchViewModel.Factory> {
            create(
                initialQuery = searchQuery,
                initialExtensionFilter = extensionFilter,
                searchAllSources = searchAllSources,
            )
        }
    val state by viewModel.state.collectAsState()
    var showSingleLoadingScreen by remember {
        mutableStateOf(searchQuery.isNotEmpty() && !extensionFilter.isNullOrEmpty() && state.total == 1)
    }

    if (showSingleLoadingScreen) {
        LoadingScreen()

        LaunchedEffect(state.items) {
            when (val result = state.items.values.singleOrNull()) {
                SearchItemResult.Loading -> return@LaunchedEffect
                is SearchItemResult.Success -> {
                    val manga = result.result.singleOrNull()
                    if (manga != null) {
                        navigator.replace(MangaRoute(manga.id, fromSource = true, fromSearch = true))
                    } else {
                        // Backoff to result screen
                        showSingleLoadingScreen = false
                    }
                }
                else -> showSingleLoadingScreen = false
            }
        }
    } else {
        GlobalSearchScreen(
            state = state,
            navigateUp = navigator::pop,
            onChangeSearchQuery = viewModel::updateSearchQuery,
            onSearch = { query ->
                viewModel.updateSearchQuery(query.trim())
                viewModel.search()
            },
            getManga = { viewModel.getManga(it) },
            onChangeSearchFilter = viewModel::setSourceFilter,
            onToggleResults = viewModel::toggleFilterResults,
            onClickSource = {
                navigator.push(BrowseSourceRoute(it.id, state.searchQuery))
            },
            onClickItem = { navigator.push(MangaRoute(it.id, fromSource = true, fromSearch = true)) },
            onLongClickItem = { navigator.push(MangaRoute(it.id, fromSource = true, fromSearch = true)) },
        )
    }
}
