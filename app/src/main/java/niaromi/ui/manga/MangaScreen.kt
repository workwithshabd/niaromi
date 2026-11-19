package niaromi.ui.manga

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEventBus
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import niaromi.core.common.util.system.logcat
import niaromi.core.navigation.BrowseSourceRoute
import niaromi.core.navigation.CategoryRoute
import niaromi.core.navigation.GlobalSearchRoute
import niaromi.core.navigation.HomeRoute
import niaromi.core.navigation.LocalAssistUrl
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.MangaNotesRoute
import niaromi.core.navigation.MangaRoute
import niaromi.core.navigation.MigrationConfigRoute
import niaromi.core.navigation.Navigator
import niaromi.core.navigation.TrackInfoDialogRoute
import niaromi.core.navigation.WebViewRoute
import niaromi.domain.chapter.model.Chapter
import niaromi.domain.manga.model.Manga
import niaromi.domain.manga.model.toSManga
import niaromi.feature.migration.dialog.MigrateMangaDialog
import niaromi.presentation.browse.components.RemoveMangaDialog
import niaromi.presentation.category.components.ChangeCategoryDialog
import niaromi.presentation.core.screens.LoadingScreen
import niaromi.presentation.core.util.isTabletUi
import niaromi.presentation.manga.ChapterSettingsDialog
import niaromi.presentation.manga.DuplicateMangaDialog
import niaromi.presentation.manga.EditCoverAction
import niaromi.presentation.manga.MangaScreen
import niaromi.presentation.manga.components.DeleteChaptersDialog
import niaromi.presentation.manga.components.MangaCoverDialog
import niaromi.presentation.manga.components.ScanlatorFilterDialog
import niaromi.presentation.manga.components.SetIntervalDialog
import niaromi.presentation.more.settings.screen.SettingsTrackingScreen
import niaromi.presentation.more.settings.screen.openSettings
import niaromi.source.isLocalOrStub
import niaromi.ui.browse.source.browse.BrowseSearchEvent
import niaromi.ui.home.TabEvent
import niaromi.ui.reader.ReaderActivity
import niaromi.util.system.copyToClipboard
import niaromi.util.system.toShareIntent
import niaromi.util.system.toast

@Composable
fun MangaScreen(
    mangaId: Long,
    fromSource: Boolean,
) {
    val navigator = LocalNavigator.current
    val resultEventBus = LocalResultEventBus.current
    val setAssistUrl = LocalAssistUrl.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isTabletUi = isTabletUi()
    val viewModel =
        assistedMetroViewModel<MangaViewModel, MangaViewModel.Factory> {
            create(mangaId = mangaId, isFromSource = fromSource)
        }

    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state is MangaViewModel.State.Loading) {
        LoadingScreen()
        return
    }

    val successState = state as MangaViewModel.State.Success
    val isHttpSource = remember { successState.source is HttpSource }

    LaunchedEffect(successState.manga, viewModel.source) {
        if (isHttpSource) {
            try {
                withContext(Dispatchers.IO) {
                    setAssistUrl(getMangaUrl(viewModel.manga, viewModel.source))
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to get manga URL" }
            }
        }
    }

    MangaScreen(
        state = successState,
        snackbarHostState = viewModel.snackbarHostState,
        nextUpdate = successState.manga.expectedNextUpdate,
        isTabletUi = isTabletUi(),
        chapterSwipeStartAction = viewModel.chapterSwipeStartAction,
        chapterSwipeEndAction = viewModel.chapterSwipeEndAction,
        navigateUp = navigator::pop,
        onChapterClicked = { openChapter(context, it) },
        onDownloadChapter = viewModel::runChapterDownloadActions.takeIf { !successState.source.isLocalOrStub() },
        downloadProgressProvider = viewModel::getDownloadProgress,
        onAddToLibraryClicked = {
            if (successState.manga.favorite) {
                viewModel.showRemoveFromLibraryDialog()
            } else {
                viewModel.toggleFavorite()
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onWebViewClicked = {
            openMangaInWebView(
                navigator,
                viewModel.manga,
                viewModel.source,
            )
        }.takeIf { isHttpSource },
        onWebViewLongClicked = {
            copyMangaUrl(
                context,
                viewModel.manga,
                viewModel.source,
            )
        }.takeIf { isHttpSource },
        onTrackingClicked = {
            if (!successState.hasLoggedInTrackers) {
                navigator.openSettings(SettingsTrackingScreen, isTabletUi)
            } else {
                navigator.push(
                    TrackInfoDialogRoute(
                        mangaId = successState.manga.id,
                        mangaTitle = successState.manga.title,
                        sourceId = successState.source.id,
                    ),
                )
            }
        },
        onTagSearch = { performGenreSearch(navigator, resultEventBus, it, viewModel.source!!) },
        onFilterButtonClicked = viewModel::showSettingsDialog,
        onRefresh = viewModel::fetchAllFromSource,
        onContinueReading = {
            val chapter = viewModel.getNextUnreadChapter() ?: successState.processedChapters.lastOrNull()?.chapter
            continueReading(context, chapter)
        },
        onSearch = { query, global -> performSearch(navigator, resultEventBus, query, global) },
        onCoverClicked = viewModel::showCoverDialog,
        onShareClicked = { shareManga(context, viewModel.manga, viewModel.source) }.takeIf { isHttpSource },
        onDownloadActionClicked = viewModel::runDownloadAction.takeIf { !successState.source.isLocalOrStub() },
        onEditCategoryClicked = viewModel::showChangeCategoryDialog.takeIf { successState.manga.favorite },
        onEditFetchIntervalClicked = viewModel::showSetFetchIntervalDialog.takeIf {
            successState.manga.favorite
        },
        onMigrateClicked = {
            navigator.push(MigrationConfigRoute(successState.manga.id))
        }.takeIf { successState.manga.favorite },
        onEditNotesClicked = { navigator.push(MangaNotesRoute(mangaId = successState.manga.id)) },
        onMultiBookmarkClicked = viewModel::bookmarkChapters,
        onMultiMarkAsReadClicked = viewModel::markChaptersRead,
        onMarkPreviousAsReadClicked = viewModel::markPreviousChapterRead,
        onMultiDeleteClicked = viewModel::showDeleteChapterDialog,
        onChapterSwipe = viewModel::chapterSwipe,
        onChapterSelected = viewModel::toggleSelection,
        onAllChapterSelected = viewModel::toggleAllSelection,
        onInvertSelection = viewModel::invertSelection,
    )

    var showScanlatorsDialog by remember { mutableStateOf(false) }

    val onDismissRequest = { viewModel.dismissDialog() }
    when (val dialog = successState.dialog) {
        null -> {}
        MangaViewModel.Dialog.RemoveFromLibrary -> {
            RemoveMangaDialog(
                onDismissRequest = onDismissRequest,
                onConfirm = {
                    if (successState.manga.favorite) viewModel.toggleFavorite()
                },
                mangaToRemove = successState.manga,
            )
        }
        is MangaViewModel.Dialog.ChangeCategory -> {
            ChangeCategoryDialog(
                initialSelection = dialog.initialSelection,
                onDismissRequest = onDismissRequest,
                onEditCategories = { navigator.push(CategoryRoute) },
                onConfirm = { include, _ ->
                    viewModel.moveMangaToCategoriesAndAddToLibrary(dialog.manga, include)
                },
                manga = dialog.manga,
            )
        }
        is MangaViewModel.Dialog.DeleteChapters -> {
            DeleteChaptersDialog(
                onDismissRequest = onDismissRequest,
                onConfirm = {
                    viewModel.toggleAllSelection(false)
                    viewModel.deleteChapters(dialog.chapters)
                },
            )
        }

        is MangaViewModel.Dialog.DuplicateManga -> {
            DuplicateMangaDialog(
                duplicates = dialog.duplicates,
                onDismissRequest = onDismissRequest,
                onConfirm = { viewModel.toggleFavorite(onRemoved = {}, checkDuplicate = false) },
                onOpenManga = { navigator.push(MangaRoute(it.id)) },
                onMigrate = { viewModel.showMigrateDialog(it) },
            )
        }

        is MangaViewModel.Dialog.Migrate -> {
            MigrateMangaDialog(
                current = dialog.current,
                target = dialog.target,
                // Initiated from the context of [dialog.target] so we show [dialog.current].
                onClickTitle = { navigator.push(MangaRoute(dialog.current.id)) },
                onDismissRequest = onDismissRequest,
            )
        }
        MangaViewModel.Dialog.SettingsSheet -> ChapterSettingsDialog(
            onDismissRequest = onDismissRequest,
            manga = successState.manga,
            downloadedOnly = successState.downloadedOnly,
            onDownloadFilterChanged = viewModel::setDownloadedFilter,
            onUnreadFilterChanged = viewModel::setUnreadFilter,
            onBookmarkedFilterChanged = viewModel::setBookmarkedFilter,
            onSortModeChanged = viewModel::setSorting,
            onDisplayModeChanged = viewModel::setDisplayMode,
            onSetAsDefault = viewModel::setCurrentSettingsAsDefault,
            onResetToDefault = viewModel::resetToDefaultSettings,
            scanlatorFilterActive = successState.scanlatorFilterActive,
            onScanlatorFilterClicked = { showScanlatorsDialog = true },
        )
        MangaViewModel.Dialog.FullCover -> {
            val vm =
                assistedMetroViewModel<MangaCoverViewModel, MangaCoverViewModel.Factory> {
                    create(mangaId = mangaId)
                }
            val manga by vm.state.collectAsStateWithLifecycle()
            if (manga != null) {
                val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                    if (it == null) return@rememberLauncherForActivityResult
                    vm.editCover(context, it)
                }
                MangaCoverDialog(
                    manga = manga!!,
                    snackbarHostState = vm.snackbarHostState,
                    isCustomCover = remember(manga) { vm.hasCustomCover() },
                    onShareClick = { vm.shareCover(context) },
                    onSaveClick = { vm.saveCover(context) },
                    onEditClick = {
                        when (it) {
                            EditCoverAction.EDIT -> getContent.launch("image/*")
                            EditCoverAction.DELETE -> vm.deleteCustomCover(context)
                            EditCoverAction.ADD_TO_LIBRARY -> vm.addToLibraryWarning(context)
                        }
                    },
                    onDismissRequest = onDismissRequest,
                )
            } else {
                LoadingScreen(Modifier.systemBarsPadding())
            }
        }
        is MangaViewModel.Dialog.SetFetchInterval -> {
            SetIntervalDialog(
                interval = dialog.manga.fetchInterval,
                nextUpdate = dialog.manga.expectedNextUpdate,
                onDismissRequest = onDismissRequest,
                onValueChanged = { interval: Int -> viewModel.setFetchInterval(dialog.manga, interval) }
                    .takeIf { viewModel.isUpdateIntervalEnabled },
            )
        }
    }

    if (showScanlatorsDialog) {
        ScanlatorFilterDialog(
            availableScanlators = successState.availableScanlators,
            excludedScanlators = successState.excludedScanlators,
            onDismissRequest = { showScanlatorsDialog = false },
            onConfirm = viewModel::setExcludedScanlators,
        )
    }
}

private fun continueReading(context: Context, unreadChapter: Chapter?) {
    if (unreadChapter != null && unreadChapter.id > 0) {
        openChapter(context, unreadChapter)
    } else {
        context.toast("No available chapters. Please re-add title or check sources.")
    }
}

private fun openChapter(context: Context, chapter: Chapter) {
    if (chapter.id > 0 && chapter.mangaId > 0) {
        context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id))
    } else {
        context.toast("Chapter unavailable. Please re-add title or check sources.")
    }
}

private fun getMangaUrl(manga_: Manga?, source_: Source?): String? {
    val manga = manga_ ?: return null
    val source = source_ as? HttpSource ?: return null

    return try {
        source.getMangaUrl(manga.toSManga())
    } catch (e: Exception) {
        null
    }
}

private fun openMangaInWebView(navigator: Navigator, manga_: Manga?, source_: Source?) {
    getMangaUrl(manga_, source_)?.let { url ->
        navigator.push(
            WebViewRoute(
                url = url,
                initialTitle = manga_?.title,
                sourceId = source_?.id,
            ),
        )
    }
}

private fun shareManga(context: Context, manga_: Manga?, source_: Source?) {
    try {
        getMangaUrl(manga_, source_)?.let { url ->
            val intent = url.toUri().toShareIntent(context, type = "text/plain")
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        context.toast(e.message)
    }
}

/**
 * Perform a search using the provided query.
 *
 * @param query the search query to the parent controller
 */
private fun performSearch(
    navigator: Navigator,
    resultEventBus: ResultEventBus,
    query: String,
    global: Boolean,
) {
    if (global) {
        navigator.push(GlobalSearchRoute(query))
        return
    }

    when (navigator.previous) {
        is HomeRoute -> {
            navigator.pop()
            resultEventBus.sendResult<TabEvent>(TabEvent.Library(query = query))
        }
        is BrowseSourceRoute -> {
            navigator.pop()
            resultEventBus.sendResult(BrowseSearchEvent(query))
        }
    }
}

/**
 * Performs a genre search using the provided genre name.
 *
 * @param genreName the search genre to the parent controller
 */
private fun performGenreSearch(
    navigator: Navigator,
    resultEventBus: ResultEventBus,
    genreName: String,
    source: Source,
) {
    if (navigator.previous is BrowseSourceRoute && source is HttpSource) {
        navigator.pop()
        resultEventBus.sendResult(BrowseSearchEvent(genreName, genre = true))
    } else {
        performSearch(navigator, resultEventBus, genreName, global = false)
    }
}

/**
 * Copy Manga URL to Clipboard
 */
private fun copyMangaUrl(context: Context, manga_: Manga?, source_: Source?) {
    val manga = manga_ ?: return
    val source = source_ as? HttpSource ?: return
    val url = source.getMangaUrl(manga.toSManga())
    context.copyToClipboard(url, url)
}
