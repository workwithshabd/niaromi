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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import kotlinx.coroutines.launch
import niaromi.domain.category.model.Category
import niaromi.domain.library.model.DiscoverySort
import niaromi.domain.library.model.LibraryManga
import niaromi.domain.manga.model.asMangaCover
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.rounded.Close
import niaromi.icons.materialsymbols.rounded.FilterList
import niaromi.icons.materialsymbols.rounded.Search
import niaromi.presentation.core.components.material.PullRefresh
import niaromi.presentation.core.components.material.rememberSpringRefreshState
import niaromi.presentation.manga.components.MangaCover
import niaromi.ui.library.ExtensionSearchResult
import niaromi.ui.library.ExtensionSearchState
import niaromi.ui.library.LibraryItem

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

@Composable
fun MangaEditorialDiscovery(
    categories: List<Category>,
    favorites: List<LibraryItem>,
    latestTitleIds: List<Long>,
    searchQuery: String?,
    contentPadding: PaddingValues,
    getItemsForCategory: (Category) -> List<LibraryItem>,
    onClickManga: (Long, Long?, String?) -> Unit,
    onViewAll: (Long, String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    sort: DiscoverySort,
    onRefresh: suspend () -> Boolean,
    onLatestChapters: () -> Unit,
    homeReset: Int,
    onGettingStarted: () -> Unit,
    modifier: Modifier = Modifier,
    extensionSearchState: ExtensionSearchState = ExtensionSearchState(),
    onAddTitle: (String) -> Unit = {},
    onViewAllSources: (String) -> Unit = {},
) {
    val sections = remember(categories, favorites, sort) {
        buildDiscoverySections(categories, favorites.map { it.libraryManga }, sort) { category ->
            getItemsForCategory(category).map { it.libraryManga }
        }
    }

    val latestTitles = remember(favorites, latestTitleIds) {
        val byId = favorites.associateBy { it.libraryManga.id }
        latestTitleIds.mapNotNull { byId[it]?.libraryManga }
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
    val refreshState = rememberSpringRefreshState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        PullRefresh(
            refreshing = false,
            enabled = searchQuery.isNullOrBlank(),
            state = refreshState,
            indicatorPadding = PaddingValues(top = contentPadding.calculateTopPadding() + 36.dp),
            onRefresh = {
                scope.launch {
                    onRefresh()
                }
            },
        ) {
            Box(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
                if (searchQuery.isNullOrBlank()) {
                    val allTitles = remember(favorites, sort) {
                        sortDiscoveryTitles(favorites.map { it.libraryManga }, sort, sectionId = 0L)
                    }
                    key(homeReset) {
                        DiscoveryHomePager(
                            sections = sections,
                            refreshState = refreshState,
                            allTitles = allTitles,
                            latestTitles = latestTitles,
                            onClickManga = onClickManga,
                            onViewAll = onViewAll,
                            onLatestChapters = onLatestChapters,
                            onSearchSources = { onAddTitle("") },
                            onGettingStarted = onGettingStarted,
                            modifier = Modifier.padding(top = SearchHeaderHeight),
                        )
                    }
                } else {
                    val listState = rememberLazyListState()
                    val scrolled by remember(listState) {
                        derivedStateOf {
                            listState.firstVisibleItemIndex > 0 ||
                                listState.firstVisibleItemScrollOffset > 0
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().background(EditorialBg).searchBackdrop(scrolled, EditorialBg),
                        contentPadding = PaddingValues(top = SearchHeaderHeight, bottom = 96.dp),
                    ) {
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
                                    onClick = { onClickManga(item.libraryManga.manga.id, null, null) },
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
                                            onClick = {
                                                onClickManga(extensionSearchState.result.manga.id, null, null)
                                            },
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
                                            onClick = {
                                                onClickManga(extensionSearchState.result.manga.id, null, null)
                                            },
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
                    }
                }
                EditorialSearchBar(
                    query = searchQuery.orEmpty(),
                    onQueryChange = onSearchQueryChange,
                    onFilterClick = onFilterClick,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
fun EditorialSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    placeholder: String = "Search your library",
    onSearch: (() -> Unit)? = null,
    filterMenu: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                        text = placeholder,
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
                    keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                androidx.compose.material3.IconButton(onClick = { onQueryChange("") }) {
                    Icon(MaterialSymbols.Rounded.Close, contentDescription = "Clear search")
                }
            }
            if (onFilterClick != null) {
                Box {
                    androidx.compose.material3.IconButton(onClick = onFilterClick) {
                        Icon(imageVector = MaterialSymbols.Rounded.FilterList, contentDescription = "Sort and filter")
                    }
                    filterMenu()
                }
            }
        }
    }
}

@Composable
fun EditorialMangaCard(
    item: LibraryManga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
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
                data = item.manga.asMangaCover(),
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.manga.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = EditorialText,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        val unread = item.unreadCount
        val subText = when {
            unread > 0 -> "$unread unread"
            item.manga.status.toInt() == 2 -> "Completed"
            else -> item.manga.author?.ifBlank { null } ?: "In Library"
        }
        Text(
            text = subText,
            style = MaterialTheme.typography.labelSmall,
            color = EditorialMuted,
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
                text = "Search all sources →",
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
            text = "Search your preferred extensions first, or look across all sources.",
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
                text = "Search extensions",
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
            text = "Search all sources →",
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
            text = "Search all sources →",
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
