package niaromi.presentation.category

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import niaromi.domain.manga.model.Manga
import niaromi.domain.manga.model.asMangaCover
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.automirroredrounded.ArrowBack
import niaromi.presentation.components.MainNavigationBar
import niaromi.presentation.core.screens.LoadingScreen
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
    val sortPreference = context.appGraph.libraryPreferences.discoverySort
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
                filterMenu = { DiscoverySortMenu(showMenu, { showMenu = false }, sortPreference::set) },
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
                    columns = GridCells.Fixed(2),
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
                    TopLevelRoute.Browse -> resultEventBus.sendResult<TabEvent>(TabEvent.Browse())
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
    subtitle: String? = item.author?.ifBlank { null } ?: item.artist,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        MangaCover.Book(
            data = item.asMangaCover(),
            contentDescription = item.title,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
        )
        Column(Modifier.padding(12.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
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
