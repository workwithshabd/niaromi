package niaromi.ui.browse.migration.manga

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.coroutines.flow.collectLatest
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.MangaRoute
import niaromi.core.navigation.MigrationConfigRoute
import niaromi.domain.manga.model.Manga
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.automirroredrounded.ArrowForward
import niaromi.icons.materialsymbols.rounded.Deselect
import niaromi.icons.materialsymbols.rounded.SelectAll
import niaromi.presentation.components.AppBar
import niaromi.presentation.components.AppBarActions
import niaromi.presentation.core.components.FastScrollLazyColumn
import niaromi.presentation.core.components.material.Scaffold
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.core.screens.EmptyScreen
import niaromi.presentation.core.screens.LoadingScreen
import niaromi.presentation.core.util.selectedBackground
import niaromi.presentation.core.util.shouldExpandFAB
import niaromi.presentation.manga.components.BaseMangaListItem
import niaromi.util.system.toast
import tachiyomi.i18n.MR

@Composable
fun MigrateMangaScreen(sourceId: Long) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val viewModel =
        assistedMetroViewModel<MigrateMangaViewModel, MigrateMangaViewModel.Factory> { create(sourceId = sourceId) }

    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        LoadingScreen()
        return
    }

    BackHandler(enabled = state.selectionMode) {
        viewModel.clearSelection()
    }

    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = if (state.selectionMode) "${state.selection.size} selected" else state.source!!.name,
                navigateUp = {
                    if (state.selectionMode) {
                        viewModel.clearSelection()
                    } else {
                        navigator.pop()
                    }
                },
                actions = {
                    if (!state.isEmpty) {
                        AppBarActions(
                            listOf(
                                if (state.selection.size == state.titles.size && state.titles.isNotEmpty()) {
                                    AppBar.Action(
                                        title = "Deselect all",
                                        icon = MaterialSymbols.Rounded.Deselect,
                                        onClick = viewModel::clearSelection,
                                    )
                                } else {
                                    AppBar.Action(
                                        title = "Select all",
                                        icon = MaterialSymbols.Rounded.SelectAll,
                                        onClick = viewModel::selectAll,
                                    )
                                },
                            ),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            SmallExtendedFloatingActionButton(
                text = { Text(text = stringResource(MR.strings.migrationConfigScreen_continueButtonText)) },
                icon = {
                    Icon(imageVector = MaterialSymbols.AutoMirroredRounded.ArrowForward, contentDescription = null)
                },
                onClick = {
                    val selection = state.selection
                    viewModel.clearSelection()
                    navigator.push(MigrationConfigRoute(selection))
                },
                expanded = lazyListState.shouldExpandFAB(),
                modifier = Modifier.animateFloatingActionButton(
                    visible = state.selectionMode,
                    alignment = Alignment.BottomEnd,
                ),
            )
        },
    ) { contentPadding ->
        if (state.isEmpty) {
            EmptyScreen(
                stringRes = MR.strings.empty_screen,
                modifier = Modifier.padding(contentPadding),
            )
            return@Scaffold
        }

        MigrateMangaContent(
            lazyListState = lazyListState,
            contentPadding = contentPadding,
            state = state,
            onClickItem = viewModel::toggleSelection,
            onClickCover = { navigator.push(MangaRoute(it.id)) },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                MigrationMangaEvent.FailedFetchingFavorites -> {
                    context.toast(MR.strings.internal_error)
                }
            }
        }
    }
}

@Composable
private fun MigrateMangaContent(
    lazyListState: LazyListState,
    contentPadding: PaddingValues,
    state: MigrateMangaViewModel.State,
    onClickItem: (Manga) -> Unit,
    onClickCover: (Manga) -> Unit,
) {
    FastScrollLazyColumn(
        state = lazyListState,
        contentPadding = contentPadding,
    ) {
        items(state.titles) { manga ->
            MigrateMangaItem(
                manga = manga,
                isSelected = manga.id in state.selection,
                onClickItem = onClickItem,
                onClickCover = onClickCover,
            )
        }
    }
}

@Composable
private fun MigrateMangaItem(
    manga: Manga,
    isSelected: Boolean,
    onClickItem: (Manga) -> Unit,
    onClickCover: (Manga) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseMangaListItem(
        modifier = modifier.selectedBackground(isSelected),
        manga = manga,
        onClickItem = { onClickItem(manga) },
        onClickCover = { onClickCover(manga) },
    )
}
