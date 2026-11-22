package niaromi.ui.updates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest
import niaromi.core.common.i18n.stringResource
import niaromi.core.navigation.DownloadQueueRoute
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.MangaRoute
import niaromi.core.navigation.UpcomingRoute
import niaromi.presentation.library.components.LATEST_CHAPTERS_SECTION_ID
import niaromi.presentation.updates.UpdateScreen
import niaromi.presentation.updates.UpdatesDeleteConfirmationDialog
import niaromi.presentation.updates.UpdatesFilterDialog
import niaromi.ui.reader.ReaderActivity
import niaromi.ui.updates.UpdatesViewModel.Event
import tachiyomi.i18n.MR

@Composable
fun LatestChaptersCollection() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val viewModel = metroViewModel<UpdatesViewModel>()
    val settingsViewModel = metroViewModel<UpdatesSettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    UpdateScreen(
        state = state,
        snackbarHostState = viewModel.snackbarHostState,
        lastUpdated = viewModel.lastUpdated,
        onBack = navigator::pop,
        onDownloadQueue = { navigator.push(DownloadQueueRoute) },
        onClickCover = { item ->
            navigator.push(
                MangaRoute(
                    item.update.mangaId,
                    categoryName = "Latest Chapters",
                    sectionId = LATEST_CHAPTERS_SECTION_ID,
                ),
            )
        },
        onSelectAll = viewModel::toggleAllSelection,
        onInvertSelection = viewModel::invertSelection,
        onUpdateLibrary = viewModel::updateLibrary,
        onDownloadChapter = viewModel::downloadChapters,
        onMultiBookmarkClicked = viewModel::bookmarkUpdates,
        onMultiMarkAsReadClicked = viewModel::markUpdatesRead,
        onMultiDeleteClicked = viewModel::showConfirmDeleteChapters,
        onUpdateSelected = viewModel::toggleSelection,
        onOpenChapter = {
            val intent = ReaderActivity.newIntent(context, it.update.mangaId, it.update.chapterId)
            context.startActivity(intent)
        },
        onCalendarClicked = { navigator.push(UpcomingRoute) },
        onFilterClicked = viewModel::showFilterDialog,
        hasActiveFilters = state.hasActiveFilters,
    )

    val onDismissDialog = { viewModel.setDialog(null) }
    when (val dialog = state.dialog) {
        is UpdatesViewModel.Dialog.DeleteConfirmation -> {
            UpdatesDeleteConfirmationDialog(
                onDismissRequest = onDismissDialog,
                onConfirm = { viewModel.deleteChapters(dialog.toDelete) },
            )
        }
        is UpdatesViewModel.Dialog.FilterSheet -> {
            UpdatesFilterDialog(
                onDismissRequest = onDismissDialog,
                viewModel = settingsViewModel,
            )
        }
        null -> {}
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                Event.InternalError -> viewModel.snackbarHostState.showSnackbar(
                    context.stringResource(MR.strings.internal_error),
                )
                is Event.LibraryUpdateTriggered -> {
                    val msg = if (event.started) {
                        MR.strings.updating_library
                    } else {
                        MR.strings.update_already_running
                    }
                    viewModel.snackbarHostState.showSnackbar(context.stringResource(msg))
                }
            }
        }
    }

    DisposableEffect(Unit) {
        viewModel.resetNewUpdatesCount()

        onDispose {
            viewModel.resetNewUpdatesCount()
        }
    }
}
