package niaromi.presentation.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.datetime.LocalDate
import niaromi.app.di.appGraph
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.MangaRoute
import niaromi.domain.history.model.HistoryWithRelations
import niaromi.domain.ui.UiPreferences
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.rounded.DeleteSweep
import niaromi.presentation.components.AppBar
import niaromi.presentation.components.AppBarActions
import niaromi.presentation.components.AppBarTitle
import niaromi.presentation.components.SearchToolbar
import niaromi.presentation.components.relativeDateText
import niaromi.presentation.core.components.FastScrollLazyColumn
import niaromi.presentation.core.components.ListGroupHeader
import niaromi.presentation.core.components.material.Scaffold
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.core.screens.EmptyScreen
import niaromi.presentation.core.screens.LoadingScreen
import niaromi.presentation.core.util.collectAsState
import niaromi.presentation.history.components.HistoryDeleteAllDialog
import niaromi.presentation.history.components.HistoryDeleteDialog
import niaromi.presentation.history.components.HistoryItem
import niaromi.presentation.util.rememberIs24HourSystem
import niaromi.ui.history.HistoryViewModel
import niaromi.ui.reader.ReaderActivity
import tachiyomi.i18n.MR

sealed interface HistoryUiModel {
    data class Header(val date: LocalDate) : HistoryUiModel
    data class Item(val item: HistoryWithRelations) : HistoryUiModel
}

@Composable
fun HistoryScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val viewModel = metroViewModel<HistoryViewModel>()
    val state by viewModel.state.collectAsState()

    HistoryScreen(
        state = state,
        snackbarHostState = viewModel.snackbarHostState,
        onSearchQueryChange = viewModel::search,
        onClickCover = { mangaId ->
            navigator.push(MangaRoute(mangaId))
        },
        onClickResume = { mangaId, chapterId ->
            context.startActivity(ReaderActivity.newIntent(context, mangaId, chapterId))
        },
        onDialogChange = viewModel::setDialog,
        navigateUp = navigator::pop,
    )

    when (val dialog = state.dialog) {
        is HistoryViewModel.Dialog.Delete -> {
            HistoryDeleteDialog(
                onDismissRequest = { viewModel.setDialog(null) },
                onDelete = { viewModel.delete(dialog.item) },
            )
        }
        is HistoryViewModel.Dialog.DeleteAll -> {
            HistoryDeleteAllDialog(
                onDismissRequest = { viewModel.setDialog(null) },
                onDelete = { viewModel.deleteAll() },
            )
        }
        null -> {}
    }
}

@Composable
fun HistoryScreen(
    state: HistoryViewModel.State,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String?) -> Unit,
    onClickCover: (mangaId: Long) -> Unit,
    onClickResume: (mangaId: Long, chapterId: Long) -> Unit,
    onDialogChange: (HistoryViewModel.Dialog?) -> Unit,
    navigateUp: () -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            SearchToolbar(
                titleContent = { AppBarTitle(stringResource(MR.strings.history)) },
                searchQuery = state.searchQuery,
                onChangeSearchQuery = onSearchQueryChange,
                actions = {
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.pref_clear_history),
                                icon = MaterialSymbols.Rounded.DeleteSweep,
                                onClick = {
                                    onDialogChange(HistoryViewModel.Dialog.DeleteAll)
                                },
                            ),
                        ),
                    )
                },
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        val list = state.list
        if (list == null) {
            LoadingScreen(Modifier.padding(contentPadding))
        } else if (list.isEmpty()) {
            val msg = if (!state.searchQuery.isNullOrEmpty()) {
                MR.strings.no_results_found
            } else {
                MR.strings.information_no_recent_manga
            }
            EmptyScreen(
                stringRes = msg,
                modifier = Modifier.padding(contentPadding),
            )
        } else {
            HistoryScreenContent(
                history = list,
                contentPadding = contentPadding,
                onClickCover = { history -> onClickCover(history.mangaId) },
                onClickResume = { history -> onClickResume(history.mangaId, history.chapterId) },
                onClickDelete = { item -> onDialogChange(HistoryViewModel.Dialog.Delete(item)) },
            )
        }
    }
}

@Composable
private fun HistoryScreenContent(
    history: List<HistoryUiModel>,
    contentPadding: PaddingValues,
    onClickCover: (HistoryWithRelations) -> Unit,
    onClickResume: (HistoryWithRelations) -> Unit,
    onClickDelete: (HistoryWithRelations) -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalLocale.current
    val is24HourSystem = rememberIs24HourSystem()
    val timeFormat by remember { context.appGraph.uiPreferences.timeFormat }.collectAsState()
    val timeFormatter = remember(timeFormat, locale, is24HourSystem) {
        UiPreferences.timeFormat(timeFormat, locale.platformLocale, is24HourSystem)
    }

    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = history,
            key = {
                when (it) {
                    is HistoryUiModel.Header -> "header-${it.date}"
                    is HistoryUiModel.Item -> "item-${it.item.id}-${it.item.chapterId}"
                }
            },
            contentType = {
                when (it) {
                    is HistoryUiModel.Header -> "header"
                    is HistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is HistoryUiModel.Header -> {
                    ListGroupHeader(
                        modifier = Modifier.animateItem(),
                        text = relativeDateText(item.date),
                    )
                }
                is HistoryUiModel.Item -> {
                    val value = item.item
                    HistoryItem(
                        modifier = Modifier.animateItem(),
                        history = value,
                        timeFormatter = timeFormatter,
                        onClickCover = { onClickCover(value) },
                        onClickResume = { onClickResume(value) },
                        onClickDelete = { onClickDelete(value) },
                    )
                }
            }
        }
    }
}
