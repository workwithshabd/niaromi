package niaromi.presentation.category

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import niaromi.app.di.appGraph
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.MangaRoute
import niaromi.domain.library.model.LibraryDisplayMode
import niaromi.domain.manga.model.Manga
import niaromi.domain.manga.model.asMangaCover
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.automirroredrounded.ArrowBack
import niaromi.presentation.components.MainNavigationBar
import niaromi.presentation.core.components.SettingsChipRow
import niaromi.presentation.core.components.SliderItem
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.core.screens.LoadingScreen
import niaromi.presentation.core.util.collectAsState
import niaromi.presentation.library.components.DiscoverySortMenu
import niaromi.presentation.library.components.EditorialSearchBar
import niaromi.presentation.library.components.LATEST_CHAPTERS_SECTION_ID
import niaromi.presentation.library.components.NO_GENRE_SECTION_ID
import niaromi.presentation.library.components.buildDiscoverySections
import niaromi.presentation.library.components.sortDiscoveryTitles
import niaromi.presentation.manga.components.MangaCover
import niaromi.ui.home.TabEvent
import niaromi.ui.home.TopLevelRoute
import niaromi.ui.library.LibrarySearchEvent
import tachiyomi.i18n.MR

private val ListingBg: Color
    @Composable get() = MaterialTheme.colorScheme.background

private val ListingText: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

private val ListingMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun CategoryListingScreen(
    categoryId: Long,
    categoryName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val resultEventBus = LocalResultEventBus.current
    val library = context.appGraph.getLibraryManga
    val libraryFlow = remember(library) { library.subscribe() }
    val libraryMangaList by libraryFlow.collectAsStateWithLifecycle(initialValue = library.cached)
    val preferences = context.appGraph.libraryPreferences
    val sortPreference = preferences.discoverySort
    val sort by sortPreference.changes().collectAsStateWithLifecycle(sortPreference.get())
    var query by rememberSaveable { mutableStateOf("") }
    val titles = remember(libraryMangaList, sort, categoryId, categoryName) {
        val section = libraryMangaList.orEmpty().filter { item ->
            when {
                categoryId == 0L -> true
                categoryId == LATEST_CHAPTERS_SECTION_ID -> item.hasLatestChapters
                categoryId > 0L -> categoryId in item.categories
                categoryId == NO_GENRE_SECTION_ID -> item.manga.primaryGenre.isNullOrBlank()
                else -> item.manga.primaryGenre?.trim().equals(categoryName, ignoreCase = true)
            }
        }
        sortDiscoveryTitles(section, sort, categoryId)
    }
    val filteredManga = remember(titles, query) {
        val search = query.trim()
        titles.map { it.manga }.filter { it.title.contains(search, true) || it.author?.contains(search, true) == true }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showDisplay by remember { mutableStateOf(false) }
    val savedDisplayMode by preferences.displayMode.collectAsState()
    val displayMode = savedDisplayMode.takeUnless { it == LibraryDisplayMode.List } ?: LibraryDisplayMode.CompactGrid
    val columnPreference = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        preferences.landscapeColumns
    } else {
        preferences.portraitColumns
    }
    val columns by columnPreference.collectAsState()

    if (showDisplay) {
        CollectionDisplayDialog(
            displayMode = displayMode,
            columns = columns,
            onSelectDisplayMode = preferences.displayMode::set,
            onColumnsChange = columnPreference::set,
            onDismiss = { showDisplay = false },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ListingBg),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            CategoryListingTopBar(
                categoryName = categoryName,
                onBack = { navigator.pop() },
            )

            EditorialSearchBar(
                query = query,
                onQueryChange = { query = it },
                onFilterClick = { showMenu = true },
                placeholder = "Search this collection",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                filterMenu = {
                    DiscoverySortMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        onSort = sortPreference::set,
                        onDisplay = { showDisplay = true },
                    )
                },
            )

            if (libraryMangaList == null) {
                LoadingScreen(Modifier.weight(1f))
            } else if (filteredManga.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = if (categoryId == LATEST_CHAPTERS_SECTION_ID) {
                                "No new chapters yet"
                            } else {
                                "No manga found"
                            },
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = ListingText,
                            ),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (categoryId == LATEST_CHAPTERS_SECTION_ID) {
                                "Titles that get updated with new chapters will appear here."
                            } else {
                                "There are no titles in this category."
                            },
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = ListingMuted,
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = if (columns > 0) {
                        GridCells.Fixed(columns.coerceIn(1, 10))
                    } else {
                        GridCells.Adaptive(if (displayMode == LibraryDisplayMode.ComfortableGrid) 140.dp else 96.dp)
                    },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    items(
                        items = filteredManga,
                        key = { it.id },
                    ) { item ->
                        CategoryMangaGridItem(
                            item = item,
                            displayMode = displayMode,
                            onClick = {
                                navigator.push(MangaRoute(item.id, categoryName = categoryName, sectionId = categoryId))
                            },
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(104.dp))
                    }
                }
            }
        }

        MainNavigationBar(
            currentRoute = TopLevelRoute.Library,
            onSelectRoute = { route ->
                when (route) {
                    TopLevelRoute.Library -> resultEventBus.sendResult(LibrarySearchEvent(""))
                    TopLevelRoute.Explore -> resultEventBus.sendResult<TabEvent>(TabEvent.Explore)
                    TopLevelRoute.More -> resultEventBus.sendResult<TabEvent>(TabEvent.More(toDownloads = false))
                }
                navigator.pop()
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun CategoryListingTopBar(
    categoryName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(categoryName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(MaterialSymbols.AutoMirroredRounded.ArrowBack, contentDescription = "Back")
            }
        },
        modifier = modifier,
    )
}

@Composable
fun CategoryMangaGridItem(
    item: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    displayMode: LibraryDisplayMode = LibraryDisplayMode.CompactGrid,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        MangaCover.Book(
            data = item.asMangaCover(),
            contentDescription = item.title,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
        )
        if (displayMode == LibraryDisplayMode.CoverOnlyGrid) return@Card
        val comfortable = displayMode == LibraryDisplayMode.ComfortableGrid
        val subtitle = item.author?.ifBlank { null } ?: item.artist
        Column(Modifier.padding(if (comfortable) 12.dp else 8.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (comfortable && !subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CollectionDisplayDialog(
    displayMode: LibraryDisplayMode,
    columns: Int,
    onSelectDisplayMode: (LibraryDisplayMode) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.action_display)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SettingsChipRow(MR.strings.action_display_mode) {
                    listOf(
                        MR.strings.action_display_grid to LibraryDisplayMode.CompactGrid,
                        MR.strings.action_display_comfortable_grid to LibraryDisplayMode.ComfortableGrid,
                        MR.strings.action_display_cover_only_grid to LibraryDisplayMode.CoverOnlyGrid,
                    ).forEach { (label, mode) ->
                        FilterChip(
                            selected = displayMode == mode,
                            label = { Text(stringResource(label)) },
                            onClick = { onSelectDisplayMode(mode) },
                        )
                    }
                }
                SliderItem(
                    value = columns.coerceIn(0, 10),
                    valueRange = 0..10,
                    label = stringResource(MR.strings.pref_library_columns),
                    valueString = if (columns > 0) columns.toString() else stringResource(MR.strings.label_auto),
                    onChange = onColumnsChange,
                    pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
