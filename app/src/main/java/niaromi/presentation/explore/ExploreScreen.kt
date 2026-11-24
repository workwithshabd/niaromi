package niaromi.presentation.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import niaromi.domain.manga.model.Manga
import niaromi.domain.manga.model.asMangaCover
import niaromi.icons.materialsymbols.rounded.Close
import niaromi.icons.materialsymbols.rounded.Explore
import niaromi.icons.materialsymbols.rounded.Refresh
import niaromi.icons.materialsymbols.rounded.Search
import niaromi.presentation.core.components.material.PullRefresh
import niaromi.presentation.core.components.material.refreshStretch
import niaromi.presentation.core.components.material.rememberRefreshPull
import niaromi.presentation.core.components.material.rememberSpringRefreshState
import niaromi.presentation.library.components.EditorialSearchBar
import niaromi.presentation.library.components.SearchHeaderHeight
import niaromi.presentation.library.components.searchBackdrop
import niaromi.presentation.manga.components.MangaCover
import niaromi.ui.browse.source.globalsearch.SearchItemResult
import niaromi.ui.browse.source.globalsearch.SearchViewModel
import niaromi.ui.explore.ExploreItem
import niaromi.ui.explore.ExploreState
import niaromi.ui.explore.SourceFilterItem

private val ExploreBg: Color
    @Composable get() = MaterialTheme.colorScheme.background

private val ExploreCardBg: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainer

private val ExploreBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant

private val ExploreText: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

private val ExploreMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val AmberGold: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Color(0xFFFBBF24)
    } else {
        Color(0xFFD97706)
    }

private val AmberBg: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Color(0xFF38230B)
    } else {
        Color(0xFFFEF3C7)
    }

@Composable
fun ExploreScreen(
    state: ExploreState,
    searchState: SearchViewModel.State,
    onMangaClick: (Manga) -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchExtensions: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState =
        androidx.compose.foundation.lazy.rememberLazyListState(),
    onLoadMore: () -> Unit = {},
) {
    val shouldLoadMore by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 6
        }
    }

    val refreshState = rememberSpringRefreshState()
    val pull = rememberRefreshPull(refreshState, state.isRefreshing)
    val query = state.searchQuery.trim()
    val searchResults = remember(searchState.items, searchState.resultsQuery, query) {
        if (searchState.resultsQuery != query) {
            emptyList()
        } else {
            searchState.items.flatMap { (source, result) ->
                (result as? SearchItemResult.Success)?.result.orEmpty().map { manga ->
                    ExploreItem(
                        manga = manga,
                        sourceId = source.id,
                        sourceName = source.name,
                        isPreferredSource = false,
                    )
                }
            }.distinctBy { it.manga.id }.chunked(3)
        }
    }
    LaunchedEffect(shouldLoadMore, query, state.isRefreshing, state.isLoadingMore) {
        if (query.isEmpty() && shouldLoadMore && !state.isLoading && !state.isLoadingMore && state.hasMorePages) {
            onLoadMore()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ExploreBg),
    ) {
        PullRefresh(
            refreshing = state.isRefreshing,
            state = refreshState,
            enabled = !state.isLoading,
            indicatorPadding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
            onRefresh = { if (query.isEmpty()) onRefresh() else onSearchExtensions(query) },
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize().statusBarsPadding().refreshStretch(pull)) {
                val scrolled by remember(listState) {
                    derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().searchBackdrop(scrolled, ExploreBg),
                    contentPadding = PaddingValues(top = SearchHeaderHeight, bottom = 100.dp),
                ) {
                    if (query.isNotEmpty()) {
                        items(searchResults, key = { "search_${it.first().manga.id}" }) { row ->
                            ExploreGridRow(
                                items = row,
                                onMangaClick = onMangaClick,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                        if (searchState.resultsQuery != query || searchState.progress < searchState.total) {
                            item(key = "search_loading") { ExploreLoadingIndicator("Searching extensions…") }
                        } else if (searchResults.isEmpty()) {
                            item(key = "search_empty") {
                                val failed = searchState.items.values.any { it is SearchItemResult.Error }
                                Text(
                                    text = when {
                                        searchState.total == 0 -> "No enabled sources"
                                        failed -> "No titles found. Some sources could not be searched."
                                        else -> "No titles found"
                                    },
                                    color = ExploreMuted,
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        return@LazyColumn
                    }

                    if (state.isLoading) {
                        item(key = "explore_loading") {
                            ExploreLoadingIndicator()
                        }
                    } else if (state.errorMessage != null && state.allItems.isEmpty()) {
                        item(key = "explore_error") {
                            ExploreEmptyState(
                                message = state.errorMessage,
                                onRetry = onRefresh,
                            )
                        }
                    } else {
                        if (state.preferredItems.isNotEmpty()) {
                            item(key = "preferred_shelf_heading") {
                                ExploreSectionHeading(
                                    title = "Preferred highlights",
                                    badgeText = "${state.preferredItems.size} TITLES",
                                    isPreferred = true,
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
                                )
                            }

                            item(key = "preferred_shelf_content") {
                                ExploreHorizontalShelf(
                                    items = state.preferredItems,
                                    onMangaClick = onMangaClick,
                                    modifier = Modifier.padding(bottom = 20.dp),
                                )
                            }
                        }

                        // Section Heading for main grid
                        item(key = "all_releases_heading") {
                            ExploreSectionHeading(
                                title = "Fresh releases",
                                badgeText = "${state.allItems.size} TITLES",
                                isPreferred = false,
                                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
                            )
                        }

                        if (state.allItems.isEmpty()) {
                            item(key = "no_filtered_results") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "No matching manga found.",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 14.sp,
                                            color = ExploreMuted,
                                        ),
                                    )
                                }
                            }
                        } else {
                            val chunked = state.allItems.chunked(3)
                            items(
                                items = chunked,
                                key = { row -> row.first().manga.id },
                            ) { rowItems ->
                                ExploreGridRow(
                                    items = rowItems,
                                    onMangaClick = onMangaClick,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                )
                            }
                        }

                        if (state.isLoadingMore) {
                            item(key = "explore_loading_more_spinner") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = ExploreText,
                                    )
                                }
                            }
                        }
                    }
                }
                EditorialSearchBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onSearch = { onSearchExtensions(query) },
                    placeholder = "Search installed extensions",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ExploreSectionHeading(
    title: String,
    badgeText: String,
    isPreferred: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isPreferred) {
                Text(
                    text = "★ ",
                    style = TextStyle(fontSize = 12.sp, color = AmberGold),
                )
            }
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = ExploreText,
                ),
            )
        }
        Text(
            text = badgeText,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                color = ExploreMuted,
            ),
        )
    }
}

@Composable
private fun ExploreHorizontalShelf(
    items: List<ExploreItem>,
    onMangaClick: (Manga) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = items,
                key = { it.manga.id },
            ) { item ->
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .clickable { onMangaClick(item.manga) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(142.dp),
                    ) {
                        MangaCover.Book(
                            data = item.manga.asMangaCover(),
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(5.dp, RoundedCornerShape(6.dp))
                                .clip(RoundedCornerShape(6.dp)),
                        )

                        // Translucent shelf accent overlapping lower part of cover
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(26.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 0.dp,
                                        topEnd = 0.dp,
                                        bottomStart = 6.dp,
                                        bottomEnd = 6.dp,
                                    ),
                                )
                                .background(Color(0x38F5A13B))
                                .border(
                                    0.5.dp,
                                    Color(0x60FFFFFF),
                                    RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp),
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.manga.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ExploreText,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreGridRow(
    items: List<ExploreItem>,
    onMangaClick: (Manga) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items.forEach { item ->
            Box(modifier = Modifier.weight(1f)) {
                ExploreGridCard(
                    item = item,
                    onClick = { onMangaClick(item.manga) },
                )
            }
        }
        // Fill remaining space if less than 3 items
        repeat(3 - items.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExploreGridCard(
    item: ExploreItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
        ) {
            MangaCover.Book(
                data = item.manga.asMangaCover(),
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
            )

            // Source Badge overlay
            val badgeBg = if (item.isPreferredSource) AmberBg else Color(0xCC171717)
            val badgeTextCol = if (item.isPreferredSource) AmberGold else Color.White
            val badgeLabel = if (item.isPreferredSource) "★ ${item.sourceName}" else item.sourceName

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeBg)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(
                    text = badgeLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeTextCol,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.manga.title,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = ExploreText,
        )
    }
}

@Composable
private fun ExploreLoadingIndicator(
    message: String = "Gathering fresh titles across extensions...",
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(message, style = MaterialTheme.typography.labelMedium, color = ExploreMuted)
        repeat(2) {
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(3) {
                    Column(Modifier.weight(1f)) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                                .background(ExploreCardBg, RoundedCornerShape(8.dp)),
                        )
                        Spacer(
                            Modifier.padding(top = 8.dp).fillMaxWidth(0.72f).height(9.dp)
                                .background(ExploreCardBg, RoundedCornerShape(5.dp)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreEmptyState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Explore,
                contentDescription = null,
                tint = ExploreMuted,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = ExploreMuted,
                ),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Retry",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
    }
}
