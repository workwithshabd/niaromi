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
import androidx.compose.foundation.layout.offset
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
import niaromi.icons.materialsymbols.rounded.FilterList
import niaromi.icons.materialsymbols.rounded.Search
import niaromi.presentation.core.components.material.PullRefresh
import niaromi.presentation.manga.components.MangaCover
import niaromi.ui.library.ExtensionSearchResult
import niaromi.ui.library.ExtensionSearchState
import niaromi.ui.library.LibraryItem
import kotlin.time.Duration.Companion.seconds

private val WarmIvoryBg = Color(0xFFFFFDF4)
private val NearBlackText = Color(0xFF171717)
private val MutedGrayText = Color(0xFF929292)
private val SearchBarBg = Color(0xFFF4F1E6)
private val SearchBarBorder = Color(0xFFE9E5D8)

data class DiscoveryCategorySection(
    val id: Long,
    val name: String,
    val coverGroups: List<DiscoveryCoverGroup>,
)

data class DiscoveryCoverGroup(
    val primaryItem: LibraryItem,
    val secondaryItems: List<LibraryItem>,
    val metadataLabel: String,
)

fun buildDiscoverySections(
    categories: List<Category>,
    favorites: List<LibraryItem>,
    getItemsForCategory: (Category) -> List<LibraryItem>,
    searchQuery: String?,
): List<DiscoveryCategorySection> {
    val query = searchQuery?.trim()
    val filterItem: (LibraryItem) -> Boolean = { item ->
        query.isNullOrEmpty() || item.libraryManga.manga.title.contains(query, ignoreCase = true)
    }

    val customCategories = categories.filterNot { it.isSystemCategory }
    val result = mutableListOf<DiscoveryCategorySection>()

    if (customCategories.isNotEmpty()) {
        customCategories.forEach { category ->
            val items = getItemsForCategory(category).filter(filterItem)
            if (items.isNotEmpty()) {
                result.add(
                    DiscoveryCategorySection(
                        id = category.id,
                        name = category.name,
                        coverGroups = createCoverGroups(items),
                    ),
                )
            }
        }
        val defaultCategory = categories.find { it.isSystemCategory }
        if (defaultCategory != null) {
            val defaultItems = getItemsForCategory(defaultCategory).filter(filterItem)
            if (defaultItems.isNotEmpty()) {
                result.add(
                    DiscoveryCategorySection(
                        id = defaultCategory.id,
                        name = "More Favourites",
                        coverGroups = createCoverGroups(defaultItems),
                    ),
                )
            }
        }
        if (result.isNotEmpty()) return result
    }

    val allFiltered = favorites.filter(filterItem)
    if (allFiltered.isEmpty()) return emptyList()

    val genreMap = mutableMapOf<String, MutableList<LibraryItem>>()
    allFiltered.forEach { item ->
        val genres = item.libraryManga.manga.genre
        if (!genres.isNullOrEmpty()) {
            genres.take(2).forEach { g ->
                genreMap.getOrPut(g.trim()) { mutableListOf() }.add(item)
            }
        }
    }

    val popularGenres = genreMap.entries
        .filter { it.value.size >= 2 }
        .sortedByDescending { it.value.size }
        .take(5)

    if (popularGenres.isNotEmpty()) {
        popularGenres.forEachIndexed { index, entry ->
            result.add(
                DiscoveryCategorySection(
                    id = index.toLong() + 100L,
                    name = entry.key,
                    coverGroups = createCoverGroups(entry.value),
                ),
            )
        }
        return result
    }

    val reading = allFiltered.filter { it.libraryManga.hasStarted && it.libraryManga.unreadCount > 0 }
    val planToRead = allFiltered.filter { !it.libraryManga.hasStarted }
    val completed = allFiltered.filter { it.libraryManga.hasStarted && it.libraryManga.unreadCount <= 0 }

    if (reading.isNotEmpty()) {
        result.add(DiscoveryCategorySection(1L, "Currently Reading", createCoverGroups(reading)))
    }
    if (planToRead.isNotEmpty()) {
        result.add(DiscoveryCategorySection(2L, "Plan to Read", createCoverGroups(planToRead)))
    }
    if (completed.isNotEmpty()) {
        result.add(DiscoveryCategorySection(3L, "Completed", createCoverGroups(completed)))
    }
    if (result.isEmpty()) {
        result.add(DiscoveryCategorySection(0L, "Favourites", createCoverGroups(allFiltered)))
    }
    return result
}

private fun createCoverGroups(items: List<LibraryItem>): List<DiscoveryCoverGroup> {
    val groups = mutableListOf<DiscoveryCoverGroup>()
    val chunkSize = 3
    val chunks = items.chunked(chunkSize)

    chunks.forEachIndexed { index, chunk ->
        val primary = chunk.first()
        val secondaries = chunk.drop(1)
        val label = when {
            index == 0 -> "<100 Ch."
            index == 1 -> "100-300 Ch."
            index == 2 -> "300> Ch."
            primary.libraryManga.unreadCount == 0L -> "Completed"
            else -> "Ongoing"
        }
        groups.add(DiscoveryCoverGroup(primary, secondaries, label))
    }
    return groups
}

@Composable
fun MangaEditorialDiscovery(
    categories: List<Category>,
    favorites: List<LibraryItem>,
    searchQuery: String?,
    contentPadding: PaddingValues,
    getItemsForCategory: (Category) -> List<LibraryItem>,
    onClickManga: (Long) -> Unit,
    onViewAll: (Long, String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onRefresh: suspend () -> Boolean,
    modifier: Modifier = Modifier,
    extensionSearchState: ExtensionSearchState = ExtensionSearchState(),
    onAddTitle: (String) -> Unit = {},
    onViewAllSources: (String) -> Unit = {},
) {
    val sections = remember(categories, favorites, searchQuery) {
        buildDiscoverySections(categories, favorites, getItemsForCategory, searchQuery)
    }

    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmIvoryBg),
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
                    if (sections.isEmpty()) {
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
                                        onClick = { onClickManga(extensionSearchState.result.manga.id) },
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
                    } else {
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
                                        onClick = { onClickManga(extensionSearchState.result.manga.id) },
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
                    }
                } else {
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
            .background(SearchBarBg, RoundedCornerShape(14.dp))
            .border(0.5.dp, SearchBarBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Search,
                contentDescription = "Search",
                tint = Color(0xFF888888),
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
                            color = Color(0xFF999999),
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
                        color = NearBlackText,
                    ),
                    cursorBrush = SolidColor(NearBlackText),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Icon(
                imageVector = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.FilterList,
                contentDescription = "Filter",
                tint = Color(0xFF888888),
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
    onClickManga: (Long) -> Unit,
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
            Text(
                text = section.name,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = NearBlackText,
                ),
            )
            Text(
                text = "View all",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = MutedGrayText,
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(
                items = section.coverGroups,
                key = { it.primaryItem.id },
            ) { group ->
                DiscoveryCoverGroupItem(
                    group = group,
                    onClick = { onClickManga(group.primaryItem.libraryManga.manga.id) },
                )
            }
        }
    }
}

@Composable
fun DiscoveryCoverGroupItem(
    group: DiscoveryCoverGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(88.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .width(88.dp)
                .height(112.dp),
        ) {
            val secondaries = group.secondaryItems
            if (secondaries.size >= 2) {
                Box(
                    modifier = Modifier
                        .offset(x = 18.dp, y = 2.dp)
                        .width(58.dp)
                        .height(90.dp)
                        .shadow(2.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE5E2D9)),
                ) {
                    MangaCover.Book(
                        data = secondaries[1].libraryManga.manga.asMangaCover(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (secondaries.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .offset(x = 9.dp, y = 5.dp)
                        .width(62.dp)
                        .height(94.dp)
                        .shadow(3.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFECE9E0)),
                ) {
                    MangaCover.Book(
                        data = secondaries[0].libraryManga.manga.asMangaCover(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = 8.dp)
                    .width(66.dp)
                    .height(98.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(4.dp),
                    )
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF0ECE3)),
            ) {
                MangaCover.Book(
                    data = group.primaryItem.libraryManga.manga.asMangaCover(),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            FrostedGlassShelfStand(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .align(Alignment.BottomCenter),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = group.metadataLabel,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = MutedGrayText,
            ),
            maxLines = 1,
        )
    }
}

@Composable
fun FrostedGlassShelfStand(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(6.dp),
            )
            .background(
                color = Color.White.copy(alpha = 0.65f),
                shape = RoundedCornerShape(6.dp),
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.90f),
                shape = RoundedCornerShape(6.dp),
            ),
    )
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
            .background(Color(0xFFF8F5EC))
            .border(0.5.dp, Color(0xFFE5E0D0), RoundedCornerShape(16.dp))
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
                    color = Color(0xFF888888),
                ),
            )
            if (!result.latestChapterName.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE8E5DA), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = result.latestChapterName,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color(0xFF1D1D1D),
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
                    .background(Color(0xFFEBE6DA)),
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
                        color = Color(0xFF171717),
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
                        color = Color(0xFF777777),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1D1D1D))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "Open Manga",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.White,
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
                    color = Color(0xFF666666),
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
            .background(Color(0xFFF8F5EC))
            .border(0.5.dp, Color(0xFFE5E0D0), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isNotInLibrary) "\"$query\" is not in your library" else "Looking for \"$query\" in extensions?",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF171717),
            ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Search all installed extensions behind the scenes to find the latest chapters.",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = Color(0xFF888888),
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1D1D1D))
                .clickable(onClick = onAddTitle)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Add title",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Color.White,
                ),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "View all sources →",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = Color(0xFF666666),
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
            .background(Color(0xFFF8F5EC))
            .border(0.5.dp, Color(0xFFE5E0D0), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = Color(0xFF1D1D1D),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Searching installed extensions for \"$query\"...",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.sp,
                color = Color(0xFF555555),
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
            .background(Color(0xFFF8F5EC))
            .border(0.5.dp, Color(0xFFE5E0D0), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No direct match found for \"$query\"",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF171717),
            ),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "View all sources →",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = Color(0xFF666666),
            ),
            modifier = Modifier
                .clickable(onClick = onViewAllSources)
                .padding(4.dp),
        )
    }
}
