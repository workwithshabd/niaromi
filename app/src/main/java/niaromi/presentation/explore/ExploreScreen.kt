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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import niaromi.presentation.manga.components.MangaCover
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
    onMangaClick: (Manga) -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectSourceFilter: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState =
        androidx.compose.foundation.lazy.rememberLazyListState(),
    onLoadMore: () -> Unit = {},
) {
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoading && !state.isLoadingMore && state.hasMorePages) {
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
            enabled = !state.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                item(key = "explore_header") {
                    ExploreHeader(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.statusBarsPadding(),
                    )
                }

                item(key = "explore_search") {
                    ExploreSearchBar(
                        query = state.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }

                if (state.availableSources.isNotEmpty()) {
                    item(key = "explore_source_chips") {
                        ExploreSourceFilterRow(
                            sources = state.availableSources,
                            selectedSourceId = state.selectedSourceId,
                            totalCount = state.allItems.size,
                            onSelectSource = onSelectSourceFilter,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
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
                    // Preferred Highlights Shelf (visible when no search/filter is active)
                    if (state.preferredItems.isNotEmpty() &&
                        state.selectedSourceId == null &&
                        state.searchQuery.isBlank()
                    ) {
                        item(key = "preferred_shelf_heading") {
                            ExploreSectionHeading(
                                title = "PREFERRED HIGHLIGHTS",
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
                        val headingTitle = if (state.selectedSourceId != null) {
                            val sourceName = state.availableSources.find { it.id == state.selectedSourceId }?.name
                            "FROM ${sourceName?.uppercase() ?: "SOURCE"}"
                        } else if (state.searchQuery.isNotBlank()) {
                            "SEARCH RESULTS"
                        } else {
                            "FRESH RELEASES"
                        }

                        ExploreSectionHeading(
                            title = headingTitle,
                            badgeText = "${state.displayedItems.size} TITLES",
                            isPreferred = false,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
                        )
                    }

                    if (state.displayedItems.isEmpty()) {
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
                        val chunked = state.displayedItems.chunked(3)
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
        }
    }
}

@Composable
private fun ExploreHeader(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 10.dp, start = 24.dp, end = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "CURATED DISCOVERIES",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    color = ExploreMuted,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "EXPLORE",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    letterSpacing = 1.sp,
                    color = ExploreText,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "New titles across extensions • No duplicates",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = ExploreMuted,
                ),
            )
        }

        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(36.dp),
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = ExploreText,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    imageVector = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Refresh,
                    contentDescription = "Refresh explore feed",
                    tint = ExploreText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ExploreSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x0A000000))
            .background(ExploreCardBg, RoundedCornerShape(16.dp))
            .border(0.5.dp, ExploreBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Search,
                contentDescription = null,
                tint = ExploreMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Filter new titles...",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            color = ExploreMuted,
                        ),
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = ExploreText,
                    ),
                    cursorBrush = SolidColor(ExploreText),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = ExploreMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreSourceFilterRow(
    sources: List<SourceFilterItem>,
    selectedSourceId: Long?,
    totalCount: Int,
    onSelectSource: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val isAllSelected = selectedSourceId == null
        SourceChip(
            label = "All ($totalCount)",
            isSelected = isAllSelected,
            isPreferred = false,
            onClick = { onSelectSource(null) },
        )

        sources.forEach { source ->
            val isSelected = selectedSourceId == source.id
            val label = if (source.isPreferred) {
                "★ ${source.name} (${source.count})"
            } else {
                "${source.name} (${source.count})"
            }
            SourceChip(
                label = label,
                isSelected = isSelected,
                isPreferred = source.isPreferred,
                onClick = { onSelectSource(source.id) },
            )
        }
    }
}

@Composable
private fun SourceChip(
    label: String,
    isSelected: Boolean,
    isPreferred: Boolean,
    onClick: () -> Unit,
) {
    val bg = when {
        isSelected -> ExploreText
        isPreferred -> AmberBg
        else -> ExploreCardBg
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.background
        isPreferred -> AmberGold
        else -> ExploreText
    }
    val borderCol = when {
        isSelected -> Color.Transparent
        isPreferred -> AmberGold.copy(alpha = 0.4f)
        else -> ExploreBorder
    }

    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(CircleShape)
            .background(bg, CircleShape)
            .border(0.5.dp, borderCol, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = if (isSelected || isPreferred) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.5.sp,
                color = textColor,
            ),
        )
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
        ) {
            MangaCover.Book(
                data = item.manga.asMangaCover(),
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(3.dp, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp)),
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 15.sp,
                color = ExploreText,
            ),
        )
    }
}

@Composable
private fun ExploreLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = ExploreText,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Gathering fresh titles across extensions...",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    color = ExploreMuted,
                ),
            )
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
