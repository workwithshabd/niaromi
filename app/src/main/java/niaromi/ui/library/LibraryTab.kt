package niaromi.ui.library

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import niaromi.app.di.appGraph
import niaromi.core.common.i18n.stringResource
import niaromi.core.navigation.CategoryListingRoute
import niaromi.core.navigation.CategoryRoute
import niaromi.core.navigation.GlobalSearchRoute
import niaromi.core.navigation.LatestChaptersRoute
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.MangaRoute
import niaromi.core.navigation.MigrationConfigRoute
import niaromi.data.library.LibraryUpdateWorker
import niaromi.domain.category.model.Category
import niaromi.domain.library.model.LibraryManga
import niaromi.domain.manga.model.Manga
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.presentation.category.components.ChangeCategoryDialog
import niaromi.presentation.core.components.material.Scaffold
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.core.screens.LoadingScreen
import niaromi.presentation.core.util.isTabletUi
import niaromi.presentation.library.DeleteLibraryMangaDialog
import niaromi.presentation.library.LibrarySettingsDialog
import niaromi.presentation.library.components.LibraryToolbar
import niaromi.presentation.library.components.MangaEditorialDiscovery
import niaromi.presentation.manga.components.LibraryBottomActionMenu
import niaromi.presentation.more.onboarding.GETTING_STARTED_URL
import niaromi.source.local.isLocal
import niaromi.ui.home.ShowBottomNavEvent
import niaromi.ui.home.TabEvent
import niaromi.ui.home.TopLevelRoute
import niaromi.ui.main.MainActivity
import niaromi.ui.reader.ReaderActivity
import niaromi.util.system.workManager
import tachiyomi.i18n.MR
import kotlin.time.Clock

@Composable
fun LibraryTab() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val resultBus = LocalResultEventBus.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var homeReset by remember { mutableIntStateOf(0) }

    val viewModel = metroViewModel<LibraryViewModel>()
    val settingsViewModel = metroViewModel<LibrarySettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val extensionSearchState by viewModel.extensionSearchState.collectAsStateWithLifecycle()

    val latestTitleIds by remember(context) {
        context.appGraph.getUpdates.subscribe(
            Clock.System.now().minus(3, DateTimeUnit.MONTH, TimeZone.currentSystemDefault()),
            unread = null,
            started = null,
            bookmarked = null,
            hideExcludedScanlators = false,
            includedCategories = emptyList(),
            excludedCategories = emptyList(),
        ).map { chapters -> chapters.map { it.mangaId }.distinct() }
    }.collectAsStateWithLifecycle(emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val statusBarColor = MaterialTheme.colorScheme.primaryContainer
    val view = LocalView.current
    DisposableEffect(view, statusBarColor) {
        val controller = (context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = statusBarColor.luminance() > 0.5f
        onDispose {
            if (previous != null) controller.isAppearanceLightStatusBars = previous
        }
    }

    val onClickRefresh: suspend (Category?) -> Boolean = { category ->
        val started = LibraryUpdateWorker.startNow(context.workManager, category)
        scope.launch {
            val msgRes = when {
                !started -> MR.strings.update_already_running
                category != null -> MR.strings.updating_category
                else -> MR.strings.updating_library
            }
            snackbarHostState.showSnackbar(context.stringResource(msgRes))
        }
        started
    }

    val sortPreference = context.appGraph.libraryPreferences.discoverySort
    val discoverySort by sortPreference.changes().collectAsStateWithLifecycle(sortPreference.get())

    Scaffold(
        topBar = { scrollBehavior ->
            if (state.selectionMode) {
                val title = state.getToolbarTitle(
                    defaultTitle = stringResource(MR.strings.label_library),
                    defaultCategoryTitle = stringResource(MR.strings.label_default),
                    page = state.coercedActiveCategoryIndex,
                )
                LibraryToolbar(
                    hasActiveFilters = state.hasActiveFilters,
                    selectedCount = state.selection.size,
                    title = title,
                    onClickUnselectAll = viewModel::clearSelection,
                    onClickSelectAll = viewModel::selectAll,
                    onClickInvertSelection = viewModel::invertSelection,
                    onClickFilter = viewModel::showSettingsDialog,
                    onClickRefresh = { scope.launch { onClickRefresh(state.activeCategory) } },
                    onClickGlobalUpdate = { scope.launch { onClickRefresh(null) } },
                    onClickOpenRandomManga = {
                        scope.launch {
                            val randomItem = viewModel.getRandomLibraryItemForCurrentCategory()
                            if (randomItem != null) {
                                navigator.push(MangaRoute(randomItem.libraryManga.manga.id))
                            } else {
                                snackbarHostState.showSnackbar(
                                    context.stringResource(MR.strings.information_no_entries_found),
                                )
                            }
                        }
                    },
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = viewModel::search,
                    scrollBehavior = scrollBehavior.takeIf { !state.showCategoryTabs },
                )
            }
        },
        bottomBar = {
            LibraryBottomActionMenu(
                visible = state.selectionMode,
                onChangeCategoryClicked = viewModel::openChangeCategoryDialog,
                onMarkAsReadClicked = { viewModel.markReadSelection(true) },
                onMarkAsUnreadClicked = { viewModel.markReadSelection(false) },
                onDownloadClicked = viewModel::performDownloadAction
                    .takeIf { state.selectedManga.fastAll { !it.isLocal() } },
                onDeleteClicked = viewModel::openDeleteMangaDialog,
                onMigrateClicked = {
                    val selection = state.selection
                    viewModel.clearSelection()
                    navigator.push(MigrationConfigRoute(selection))
                },
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = if (isTabletUi()) 8.dp else 88.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) { contentPadding ->
        when {
            state.isLoading -> {
                LoadingScreen(Modifier.padding(contentPadding))
            }
            else -> {
                MangaEditorialDiscovery(
                    categories = state.displayedCategories,
                    favorites = state.libraryData.favorites,
                    latestTitleIds = latestTitleIds,
                    searchQuery = state.searchQuery,
                    extensionSearchState = extensionSearchState,
                    contentPadding = contentPadding,
                    getItemsForCategory = { state.getItemsForCategory(it) },
                    onClickManga = { mangaId, sectionId, sectionName ->
                        navigator.push(
                            MangaRoute(
                                mangaId = mangaId,
                                fromSearch = !state.searchQuery.isNullOrBlank(),
                                categoryName = sectionName,
                                sectionId = sectionId,
                            ),
                        )
                    },
                    onViewAll = { id, name -> navigator.push(CategoryListingRoute(id, name)) },
                    onSearchQueryChange = viewModel::search,
                    onAddTitle = { query -> navigator.push(GlobalSearchRoute(query)) },
                    onViewAllSources = { query -> navigator.push(GlobalSearchRoute(query, searchAllSources = true)) },
                    onFilterClick = viewModel::showSettingsDialog,
                    sort = discoverySort,
                    onRefresh = { onClickRefresh(null) },
                    onLatestChapters = { navigator.push(LatestChaptersRoute) },
                    homeReset = homeReset,
                    onGettingStarted = { uriHandler.openUri(GETTING_STARTED_URL) },
                )
            }
        }
    }

    val onDismissRequest = viewModel::closeDialog
    when (val dialog = state.dialog) {
        is LibraryViewModel.Dialog.SettingsSheet -> run {
            LibrarySettingsDialog(
                onDismissRequest = onDismissRequest,
                viewModel = settingsViewModel,
            )
        }
        is LibraryViewModel.Dialog.ChangeCategory -> {
            ChangeCategoryDialog(
                initialSelection = dialog.initialSelection,
                onDismissRequest = onDismissRequest,
                onEditCategories = {
                    viewModel.clearSelection()
                    navigator.push(CategoryRoute)
                },
                onConfirm = { include, exclude ->
                    viewModel.clearSelection()
                    viewModel.setMangaCategories(dialog.manga, include, exclude)
                },
            )
        }
        is LibraryViewModel.Dialog.DeleteManga -> {
            DeleteLibraryMangaDialog(
                containsLocalManga = dialog.manga.any(Manga::isLocal),
                onDismissRequest = onDismissRequest,
                onConfirm = { deleteManga, deleteChapter ->
                    viewModel.removeMangas(dialog.manga, deleteManga, deleteChapter)
                    viewModel.clearSelection()
                },
            )
        }
        null -> {}
    }

    BackHandler(enabled = state.selectionMode || state.searchQuery != null) {
        when {
            state.selectionMode -> viewModel.clearSelection()
            state.searchQuery != null -> viewModel.search(null)
        }
    }

    LaunchedEffect(state.isLoading, state.selectionMode) {
        if (!state.isLoading) {
            resultBus.sendResult(
                result = ShowBottomNavEvent(!state.selectionMode),
            )
            (context as? MainActivity)?.ready = true
        }
    }
    // Lets startup be measured up to the library, not just its loading screen
    ReportDrawnWhen { !state.isLoading }

    ResultEffect<LibrarySearchEvent> {
        viewModel.search(it.query.ifBlank { null })
    }

    ResultEffect<Unit>(resultKey = TopLevelRoute.Library.reselectEventKey) {
        viewModel.search(null)
        homeReset++
    }
}

data class LibrarySearchEvent(val query: String)
