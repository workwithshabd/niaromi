package niaromi.presentation.updates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import niaromi.data.download.model.Download
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.rounded.CalendarMonth
import niaromi.icons.materialsymbols.rounded.FilterList
import niaromi.icons.materialsymbols.rounded.FlipToBack
import niaromi.icons.materialsymbols.rounded.Refresh
import niaromi.icons.materialsymbols.rounded.SelectAll
import niaromi.presentation.components.AppBar
import niaromi.presentation.components.AppBarActions
import niaromi.presentation.core.components.FastScrollLazyColumn
import niaromi.presentation.core.components.material.PullRefresh
import niaromi.presentation.core.components.material.Scaffold
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.core.screens.EmptyScreen
import niaromi.presentation.core.screens.LoadingScreen
import niaromi.presentation.core.theme.active
import niaromi.presentation.library.components.EditorialSearchBar
import niaromi.presentation.manga.components.ChapterDownloadAction
import niaromi.presentation.manga.components.MangaBottomActionMenu
import niaromi.presentation.manga.components.MangaCover
import niaromi.ui.updates.UpdatesItem
import niaromi.ui.updates.UpdatesViewModel
import tachiyomi.i18n.MR
import kotlin.time.Duration.Companion.seconds

@Composable
fun UpdateScreen(
    state: UpdatesViewModel.State,
    snackbarHostState: SnackbarHostState,
    lastUpdated: Long,
    onBack: () -> Unit,
    onDownloadQueue: () -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onCalendarClicked: () -> Unit,
    onUpdateLibrary: suspend () -> Boolean,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<UpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<UpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<UpdatesItem>) -> Unit,
    onUpdateSelected: (UpdatesItem, Boolean, Boolean) -> Unit,
    onOpenChapter: (UpdatesItem) -> Unit,
    onFilterClicked: () -> Unit,
    hasActiveFilters: Boolean,
) {
    BackHandler(enabled = state.selectionMode) {
        onSelectAll(false)
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { scrollBehavior ->
            UpdatesAppBar(
                onBack = onBack,
                onDownloadQueue = onDownloadQueue,
                onCalendarClicked = { onCalendarClicked() },
                onUpdateLibrary = { scope.launch { onUpdateLibrary() } },
                onFilterClicked = { onFilterClicked() },
                hasFilters = hasActiveFilters,
                actionModeCounter = state.selected.size,
                onSelectAll = { onSelectAll(true) },
                onInvertSelection = { onInvertSelection() },
                onCancelActionMode = { onSelectAll(false) },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            UpdatesBottomBar(
                selected = state.selected,
                onDownloadChapter = onDownloadChapter,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                onMultiDeleteClicked = onMultiDeleteClicked,
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding(),
            )
        },
    ) { contentPadding ->
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
            else -> {
                var query by remember { mutableStateOf("") }
                var chapterMenuId by remember { mutableStateOf<Long?>(null) }
                val groups = remember(state.items, query) {
                    val search = query.trim()
                    state.items.filter {
                        it.update.mangaTitle.contains(search, true) || it.update.chapterName.contains(search, true)
                    }.groupBy { it.update.mangaId }.values.toList()
                }
                Column(
                    Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding()),
                ) {
                    EditorialSearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = "Search latest chapters",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                    PullRefresh(
                        refreshing = false,
                        onRefresh = { scope.launch { onUpdateLibrary() } },
                        enabled = !state.selectionMode,
                        modifier = Modifier.weight(1f),
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(96.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = contentPadding.calculateBottomPadding() + 12.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item(key = "last_update", span = { GridItemSpan(maxLineSpan) }) {
                                UpdatesLastUpdatedHeader(lastUpdated = lastUpdated)
                            }
                            if (groups.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyScreen(
                                        stringRes = if (hasActiveFilters || query.isNotBlank()) {
                                            MR.strings.error_no_match
                                        } else {
                                            MR.strings.information_no_recent
                                        },
                                        modifier = Modifier.height(280.dp),
                                    )
                                }
                            }
                            items(groups, key = { it.first().update.mangaId }) { chapters ->
                                val first = chapters.first()
                                val selected = chapters.any { it.selected }
                                val shape = MaterialTheme.shapes.extraSmall
                                Card(
                                    shape = shape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            Color.Transparent
                                        },
                                    ),
                                    modifier = Modifier
                                        .clip(shape)
                                        .animateItem()
                                        .combinedClickable(
                                            onClick = {
                                                if (state.selectionMode) {
                                                    chapters.forEach { onUpdateSelected(it, !selected, false) }
                                                } else {
                                                    onClickCover(first)
                                                }
                                            },
                                            onLongClick = { chapterMenuId = first.update.mangaId },
                                        ),
                                ) {
                                    MangaCover.Book(
                                        data = first.update.coverData,
                                        modifier = Modifier.fillMaxWidth(),
                                        contentDescription = first.update.mangaTitle,
                                        shape = MaterialTheme.shapes.extraSmall,
                                    )
                                    Column(Modifier.padding(8.dp)) {
                                        Text(
                                            first.update.mangaTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            minLines = 2,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            if (chapters.size == 1) "1 chapter" else "${chapters.size} chapters",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                val menuChapters = state.items.filter { it.update.mangaId == chapterMenuId }
                if (menuChapters.isNotEmpty()) {
                    AlertDialog(
                        onDismissRequest = { chapterMenuId = null },
                        title = { Text(menuChapters.first().update.mangaTitle) },
                        text = {
                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                menuChapters.forEach { entry ->
                                    CollectionChapterRow(
                                        updatesItem = entry,
                                        selectionMode = state.selectionMode,
                                        onUpdateSelected = onUpdateSelected,
                                        onClickUpdate = {
                                            chapterMenuId = null
                                            onOpenChapter(it)
                                        },
                                        onDownloadChapter = onDownloadChapter,
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { chapterMenuId = null }) { Text("Done") }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdatesAppBar(
    onBack: () -> Unit,
    onDownloadQueue: () -> Unit,
    onCalendarClicked: () -> Unit,
    onUpdateLibrary: () -> Unit,
    onFilterClicked: () -> Unit,
    hasFilters: Boolean,
    // For action mode
    actionModeCounter: Int,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCancelActionMode: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    AppBar(
        modifier = modifier,
        title = "Latest Chapters",
        navigateUp = onBack,
        actions = {
            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        MaterialSymbols.Rounded.FilterList,
                        contentDescription = "Update options",
                        tint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current,
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf(
                        stringResource(MR.strings.action_filter) to onFilterClicked,
                        stringResource(MR.strings.action_view_upcoming) to onCalendarClicked,
                        stringResource(MR.strings.action_update_library) to onUpdateLibrary,
                        stringResource(MR.strings.label_download_queue) to onDownloadQueue,
                    ).forEach { (label, action) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                expanded = false
                                action()
                            },
                        )
                    }
                }
            }
        },
        actionModeCounter = actionModeCounter,
        onCancelActionMode = onCancelActionMode,
        actionModeActions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = MaterialSymbols.Rounded.SelectAll,
                        onClick = onSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = MaterialSymbols.Rounded.FlipToBack,
                        onClick = onInvertSelection,
                    ),
                ),
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun UpdatesBottomBar(
    selected: List<UpdatesItem>,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<UpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<UpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<UpdatesItem>) -> Unit,
) {
    MangaBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, true)
        }.takeIf { selected.fastAny { !it.update.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, false)
        }.takeIf { selected.fastAll { it.update.bookmark } },
        onMarkAsReadClicked = {
            onMultiMarkAsReadClicked(selected, true)
        }.takeIf { selected.fastAny { !it.update.read } },
        onMarkAsUnreadClicked = {
            onMultiMarkAsReadClicked(selected, false)
        }.takeIf { selected.fastAny { it.update.read || it.update.lastPageRead > 0 } },
        onDownloadClicked = {
            onDownloadChapter(selected, ChapterDownloadAction.START)
        }.takeIf {
            selected.fastAny { it.downloadStateProvider() != Download.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected)
        }.takeIf { selected.fastAny { it.downloadStateProvider() == Download.State.DOWNLOADED } },
    )
}

sealed interface UpdatesUiModel {
    data class Header(val date: LocalDate) : UpdatesUiModel
    data class Item(val item: UpdatesItem) : UpdatesUiModel
}
