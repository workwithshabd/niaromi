package niaromi.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import niaromi.domain.category.model.Category
import niaromi.domain.manga.model.asMangaCover
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.rounded.FilterList
import niaromi.icons.materialsymbols.rounded.Search
import niaromi.presentation.core.components.material.PullRefresh
import niaromi.presentation.manga.components.MangaCover
import niaromi.ui.library.ExtensionSearchResult
import niaromi.ui.library.ExtensionSearchState
import niaromi.ui.library.LibraryItem
import kotlin.time.Duration.Companion.seconds

private val EditorialBg: Color
    @Composable get() = MaterialTheme.colorScheme.background

private val EditorialText: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

private val EditorialMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val EditorialCardBg: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainer

private val EditorialCardBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant

data class DiscoveryCategorySection(
    val id: Long,
    val name: String,
    val items: List<LibraryItem>,
)

fun buildDiscoverySections(
    categories: List<Category>,
    favorites: List<LibraryItem>,
    getItemsForCategory: (Category) -> List<LibraryItem>,
    searchQuery: String?,
): List<DiscoveryCategorySection> {
    val query = searchQuery?.trim()
    val filterItem: (LibraryItem) -> Boolean = { item ->
        query.isNullOrEmpty() ||
            item.libraryManga.manga.title.contains(query, ignoreCase = true) ||
            item.libraryManga.manga.author?.contains(query, ignoreCase = true) == true
    }

    val filteredFavorites = favorites.filter(filterItem)
    if (filteredFavorites.isEmpty()) return emptyList()

    val sortByLastRead: (List<LibraryItem>) -> List<LibraryItem> = { list ->
        list.sortedWith(
            compareByDescending<LibraryItem> { it.libraryManga.lastRead?.toEpochMilliseconds() ?: 0L }
                .thenByDescending { it.libraryManga.manga.favoriteAt?.toEpochMilliseconds() ?: 0L },
        )
    }

    val result = mutableListOf<DiscoveryCategorySection>()
    val seenSectionNames = mutableSetOf<String>()
    val assignedMangaIds = mutableSetOf<Long>()

    // 1. Custom categories (if user defined categories exist)
    val customCategories = categories.filterNot { it.isSystemCategory }
    if (customCategories.isNotEmpty()) {
        customCategories.forEach { category ->
            val items = getItemsForCategory(category)
                .filter(filterItem)
                .filter { it.id !in assignedMangaIds }
            if (items.isNotEmpty()) {
                result.add(
                    DiscoveryCategorySection(
                        id = category.id,
                        name = category.name,
                        items = sortByLastRead(items),
                    ),
                )
                seenSectionNames.add(category.name.lowercase())
                items.forEach { assignedMangaIds.add(it.id) }
            }
        }
    }

    // 2. Unassigned titles grouped by their primary genre only (strictly one category/genre per title)
    val unassignedTitles = filteredFavorites.filter { it.id !in assignedMangaIds }
    val genreMap = linkedMapOf<String, MutableList<LibraryItem>>()

    unassignedTitles.forEach { item ->
        val genres = item.libraryManga.manga.genre
        val primaryGenre = genres?.firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        if (!primaryGenre.isNullOrBlank()) {
            genreMap.getOrPut(primaryGenre) { mutableListOf() }.add(item)
        }
    }

    // Sort genres by number of titles descending, then by name
    val sortedGenres = genreMap.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, MutableList<LibraryItem>>> {
                it.value.size
            }.thenBy { it.key },
        )

    var genreSectionId = 1000L
    sortedGenres.forEach { (genreName, items) ->
        if (genreName.lowercase() !in seenSectionNames && items.isNotEmpty()) {
            result.add(
                DiscoveryCategorySection(
                    id = genreSectionId++,
                    name = genreName,
                    items = sortByLastRead(items),
                ),
            )
            seenSectionNames.add(genreName.lowercase())
            items.forEach { assignedMangaIds.add(it.id) }
        }
    }

    // 3. Any titles that don't have genres or didn't fall into the above sections
    val remainingTitles = filteredFavorites.filter { it.id !in assignedMangaIds }
    if (remainingTitles.isNotEmpty()) {
        result.add(
            DiscoveryCategorySection(
                id = 9999L,
                name = if (result.isEmpty()) "All Titles" else "More Favourites",
                items = sortByLastRead(remainingTitles),
            ),
        )
    }

    if (result.isEmpty()) {
        result.add(
            DiscoveryCategorySection(
                id = 0L,
                name = "All Titles",
                items = sortByLastRead(filteredFavorites),
            ),
        )
    }

    return result
}

@Composable
fun MangaEditorialDiscovery(
    categories: List<Category>,
    favorites: List<LibraryItem>,
    searchQuery: String?,
    contentPadding: PaddingValues,
    getItemsForCategory: (Category) -> List<LibraryItem>,
    onClickManga: (Long, String) -> Unit,
    onViewAll: (Long, String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onRefresh: suspend () -> Boolean,
    modifier: Modifier = Modifier,
    lazyListState: androidx.compose.foundation.lazy.LazyListState =
        androidx.compose.foundation.lazy.rememberLazyListState(),
    extensionSearchState: ExtensionSearchState = ExtensionSearchState(),
    onAddTitle: (String) -> Unit = {},
    onViewAllSources: (String) -> Unit = {},
) {
    val sections = remember(categories, favorites, searchQuery) {
        buildDiscoverySections(categories, favorites, getItemsForCategory, searchQuery)
    }

    val matchingFavorites = remember(favorites, searchQuery) {
        val q = searchQuery?.trim()
        if (q.isNullOrBlank()) {
            emptyList()
        } else {
            favorites.filter {
                it.libraryManga.manga.title.contains(q, ignoreCase = true) ||
                    it.libraryManga.manga.author?.contains(q, ignoreCase = true) == true
            }
        }
    }

    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EditorialBg),
    ) {
        PullRefresh(
            refreshing = isRefreshing,
            enabled = true,
            onRefresh = {
                scope.launch {
                    if (!onRefresh()) return@launch
                    isRefreshing = true
                    delay(1.seconds)
                    isRefreshing = false
                }
            },
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = 96.dp,
                ),
            ) {
                item(key = "top_search_bar") {
                    EditorialSearchBar(
                        query = searchQuery ?: "",
                        onQueryChange = onSearchQueryChange,
                        onFilterClick = onFilterClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }

                if (!searchQuery.isNullOrBlank()) {
                    // Search is active
                    if (matchingFavorites.isNotEmpty()) {
                        item(key = "search_library_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "IN YOUR LIBRARY (${matchingFavorites.size})",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 1.sp,
                                        color = EditorialText,
                                    ),
                                )
                            }
                        }

                        items(
                            items = matchingFavorites,
                            key = { "fav_search_${it.id}" },
                        ) { item ->
                            EditorialSearchResultItem(
                                item = item,
                                onClick = { onClickManga(item.libraryManga.manga.id, "") },
                            )
                        }

                        item(key = "search_extension_additional_action") {
                            when {
                                extensionSearchState.isSearching -> {
                                    ExtensionSearchingCard(
                                        query = searchQuery,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                    )
                                }
                                extensionSearchState.result != null -> {
                                    ExtensionFoundMangaCard(
                                        result = extensionSearchState.result,
                                        onClick = { onClickManga(extensionSearchState.result.manga.id, "") },
                                        onViewAllSources = { onViewAllSources(searchQuery) },
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                    )
                                }
                                else -> {
                                    AddTitlePromptCard(
                                        query = searchQuery,
                                        onAddTitle = { onAddTitle(searchQuery) },
                                        onViewAllSources = { onViewAllSources(searchQuery) },
                                        isNotInLibrary = false,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        // Not found in local library
                        item(key = "search_extension_empty_state") {
                            when {
                                extensionSearchState.isSearching -> {
                                    ExtensionSearchingCard(
                                        query = searchQuery,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                                    )
                                }
                                extensionSearchState.result != null -> {
                                    ExtensionFoundMangaCard(
                                        result = extensionSearchState.result,
                                        onClick = { onClickManga(extensionSearchState.result.manga.id, "") },
                                        onViewAllSources = { onViewAllSources(searchQuery) },
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                    )
                                }
                                extensionSearchState.hasSearched -> {
                                    NoExtensionResultsCard(
                                        query = searchQuery,
                                        onViewAllSources = { onViewAllSources(searchQuery) },
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                                    )
                                }
                                else -> {
                                    AddTitlePromptCard(
                                        query = searchQuery,
                                        onAddTitle = { onAddTitle(searchQuery) },
                                        onViewAllSources = { onViewAllSources(searchQuery) },
                                        isNotInLibrary = true,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Normal feed - show all genre and category sections
                    items(
                        items = sections,
                        key = { it.id },
                    ) { section ->
                        DiscoveryCategoryRow(
                            section = section,
                            onClickManga = onClickManga,
                            onViewAll = { onViewAll(section.id, section.name) },
                            modifier = Modifier.padding(bottom = 24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorialSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(EditorialCardBg, RoundedCornerShape(14.dp))
            .border(0.5.dp, EditorialCardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Search,
                contentDescription = "Search",
                tint = EditorialMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search any manga...",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            color = EditorialMuted,
                        ),
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = EditorialText,
                    ),
                    cursorBrush = SolidColor(EditorialText),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Icon(
                imageVector = MaterialSymbols.Rounded.FilterList,
                contentDescription = "Filter",
                tint = EditorialMuted,
                modifier = Modifier
                    .size(19.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onFilterClick,
                    ),
            )
        }
    }
}

@Composable
fun DiscoveryCategoryRow(
    section: DiscoveryCategorySection,
    onClickManga: (Long, String) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = section.name,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = EditorialText,
                    ),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${section.items.size}",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = EditorialMuted,
                    ),
                )
            }
            Text(
                text = "View all",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = EditorialMuted,
                ),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onViewAll,
                ),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(
                items = section.items.take(5),
                key = { "${section.id}_${it.id}" },
            ) { item ->
                EditorialMangaCard(
                    item = item,
                    onClick = { onClickManga(item.libraryManga.manga.id, section.name) },
                )
            }
        }
    }
}

@Composable
fun EditorialMangaCard(
    item: LibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .width(105.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .width(105.dp)
                .height(150.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(8.dp),
                    ambientColor = Color(0x20000000),
                    spotColor = Color(0x30000000),
                )
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            MangaCover.Book(
                data = item.libraryManga.manga.asMangaCover(),
                modifier = Modifier.fillMaxSize(),
                onClick = onClick,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.libraryManga.manga.title,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = EditorialText,
                lineHeight = 16.sp,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        val unread = item.unreadCount
        val subText = when {
            unread > 0 -> "$unread unread"
            item.libraryManga.manga.status.toInt() == 2 -> "Completed"
            else -> item.libraryManga.manga.author?.ifBlank { null } ?: "In Library"
        }
        Text(
            text = subText,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = EditorialMuted,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun EditorialSearchResultItem(
    item: LibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(EditorialCardBg)
            .border(0.5.dp, EditorialCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(74.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(EditorialCardBorder),
        ) {
            MangaCover.Book(
                data = item.libraryManga.manga.asMangaCover(),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.libraryManga.manga.title,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = EditorialText,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            val genreText = item.libraryManga.manga.genre?.take(3)?.joinToString(" • ")
            if (!genreText.isNullOrBlank()) {
                Text(
                    text = genreText,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = Color(0xFFD97706),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            val author = item.libraryManga.manga.author?.ifBlank { null } ?: "In Library"
            Text(
                text = author,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = EditorialMuted,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ExtensionFoundMangaCard(
    result: ExtensionSearchResult,
    onClick: () -> Unit,
    onViewAllSources: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EditorialCardBg)
            .border(0.5.dp, EditorialCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "FOUND IN ${result.sourceName.uppercase()}",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = EditorialMuted,
                ),
            )
            if (!result.latestChapterName.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .background(EditorialCardBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = result.latestChapterName,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = EditorialText,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(86.dp)
                    .shadow(3.dp, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .background(EditorialCardBorder),
            ) {
                MangaCover.Book(
                    data = result.manga.asMangaCover(),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.manga.title,
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = EditorialText,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.manga.author ?: result.sourceName,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = EditorialMuted,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onClick)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "Open Manga",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onViewAllSources)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "View all sources →",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = EditorialMuted,
                ),
            )
        }
    }
}

@Composable
fun AddTitlePromptCard(
    query: String,
    onAddTitle: () -> Unit,
    onViewAllSources: () -> Unit,
    isNotInLibrary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EditorialCardBg)
            .border(0.5.dp, EditorialCardBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isNotInLibrary) "\"$query\" is not in your library" else "Looking for \"$query\" in extensions?",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = EditorialText,
            ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Search all installed extensions behind the scenes to find the latest chapters.",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = EditorialMuted,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onAddTitle)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Add title",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "View all sources →",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = EditorialMuted,
            ),
            modifier = Modifier
                .clickable(onClick = onViewAllSources)
                .padding(4.dp),
        )
    }
}

@Composable
fun ExtensionSearchingCard(
    query: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(EditorialCardBg)
            .border(0.5.dp, EditorialCardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Searching installed extensions for \"$query\"...",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.sp,
                color = EditorialMuted,
            ),
        )
    }
}

@Composable
fun NoExtensionResultsCard(
    query: String,
    onViewAllSources: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EditorialCardBg)
            .border(0.5.dp, EditorialCardBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No direct match found for \"$query\"",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = EditorialText,
            ),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "View all sources →",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = EditorialMuted,
            ),
            modifier = Modifier
                .clickable(onClick = onViewAllSources)
                .padding(4.dp),
        )
    }
}
