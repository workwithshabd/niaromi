package niaromi.presentation.library.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import niaromi.domain.library.model.LibraryManga
import niaromi.domain.manga.model.asMangaCover
import niaromi.presentation.components.DropdownMenu
import niaromi.presentation.core.components.material.SpringRefreshState
import niaromi.presentation.core.components.material.rememberRefreshPull
import niaromi.presentation.manga.components.MangaCover
import kotlin.math.roundToInt

@Composable
fun DiscoveryHomePager(
    sections: List<DiscoveryCategorySection>,
    refreshState: SpringRefreshState,
    allTitles: List<LibraryManga>,
    latestTitles: List<LibraryManga>,
    onClickManga: (Long, Long?, String?) -> Unit,
    onViewAll: (Long, String) -> Unit,
    onLatestChapters: () -> Unit,
    onSearchSources: () -> Unit,
    onGettingStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = remember(sections, allTitles) {
        val browsable = sections.filterNot {
            it.id == LATEST_CHAPTERS_SECTION_ID ||
                (it.id == NO_GENRE_SECTION_ID && it.items.size == allTitles.size)
        }
        listOf(DiscoveryCategorySection(0L, "All titles", allTitles)) +
            browsable.filter { it.id < 0L } + browsable.filter { it.id > 0L }
    }
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val panel = MaterialTheme.colorScheme.surface
    val pull by rememberRefreshPull(refreshState)
    val selectedPage = pages[pager.currentPage.coerceIn(pages.indices)]
    val heroSection =
        remember(latestTitles) { DiscoveryCategorySection(LATEST_CHAPTERS_SECTION_ID, "Latest Chapters", latestTitles) }
    var sectionMenuOpen by remember { mutableStateOf(false) }
    var collapsed by remember { mutableFloatStateOf(0f) }
    var collapseAnimation by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(pager.currentPage, pages.size) {
        if (pager.currentPage !in pages.indices) {
            pager.scrollToPage(pager.currentPage.coerceIn(pages.indices))
        }
    }

    val heroColor = MaterialTheme.colorScheme.primaryContainer
    val backdropColor = lerp(heroColor, panel, collapsed)
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().clipToBounds().background(backdropColor)
            .searchBackdrop(collapsed > 0f),
    ) {
        val availableHeight = (maxHeight - SearchHeaderHeight).coerceAtLeast(0.dp)
        val heroHeight = (availableHeight * 0.46f).coerceAtMost(350.dp)
        val heroPixels = with(LocalDensity.current) { heroHeight.toPx().coerceAtLeast(1f) }
        val movePanel = remember(heroPixels) {
            { pixels: Float ->
                collapseAnimation?.cancel()
                val before = collapsed
                collapsed = (before - pixels / heroPixels).coerceIn(0f, 1f)
                (before - collapsed) * heroPixels
            }
        }
        val scroll = remember(movePanel) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                    Offset(0f, movePanel(available.y))
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = SearchHeaderHeight)
                .height(heroHeight)
                .nestedScroll(scroll)
                .scrollable(
                    state = rememberScrollableState { 0f },
                    orientation = Orientation.Vertical,
                )
                .graphicsLayer {
                    translationY = -(heroPixels * 0.15f + SearchHeaderHeight.toPx()) * collapsed + 22.dp.toPx() * pull
                    alpha = 1f - collapsed
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    scaleX = 1f - 0.025f * pull
                    scaleY = 1f + 0.045f * pull
                },
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            LatestChaptersSummary(
                section = heroSection,
                onClickManga = { onClickManga(it.id, heroSection.id, heroSection.name) },
                onOpen = onLatestChapters,
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .height(availableHeight)
                .offset {
                    val panelTop = SearchHeaderHeight.toPx() + heroPixels * (1f - collapsed) + 28.dp.toPx() * pull
                    IntOffset(0, panelTop.roundToInt())
                }
                .nestedScroll(scroll)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(panel),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .scrollable(rememberScrollableState { 0f }, Orientation.Vertical)
                    .padding(start = 12.dp, end = 12.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    TextButton(onClick = { sectionMenuOpen = true }) {
                        Text(
                            text = selectedPage.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text("▾", Modifier.padding(start = 6.dp))
                    }
                    DropdownMenu(expanded = sectionMenuOpen, onDismissRequest = { sectionMenuOpen = false }) {
                        pages.forEachIndexed { index, page ->
                            val isCollection = page.id > 0L
                            val isFirstInGroup = index > 0 &&
                                (index == 1 || (pages[index - 1].id > 0L) != isCollection)
                            if (isFirstInGroup) {
                                Text(
                                    text = if (isCollection) "Collections" else "Genres",
                                    modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(page.name) },
                                onClick = {
                                    sectionMenuOpen = false
                                    collapseAnimation?.cancel()
                                    collapseAnimation = scope.launch {
                                        animate(collapsed, 1f, animationSpec = tween(260)) { value, _ ->
                                            collapsed = value
                                        }
                                    }
                                    scope.launch {
                                        pager.animateScrollToPage(index, animationSpec = tween(240))
                                    }
                                },
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { onViewAll(selectedPage.id, selectedPage.name) },
                    enabled = selectedPage.items.isNotEmpty(),
                ) { Text("View all") }
            }
            HorizontalPager(
                state = pager,
                key = { index -> pages.getOrNull(index)?.navigationKey() ?: "pending-page:$index" },
                verticalAlignment = Alignment.Top,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { index ->
                val page = pages.getOrNull(index) ?: return@HorizontalPager
                val featured = remember(page.items) { page.items.take(4).takeIf { it.size == 4 }.orEmpty() }
                val rows = remember(page.items, featured.size) {
                    page.items.take(HOME_PREVIEW_SIZE).drop(featured.size).chunked(HOME_GRID_COLUMNS)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    if (featured.isNotEmpty()) {
                        item(key = "featured") {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(HOME_GRID_GAP),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (page.id > 0L) "Collection" else "From your library",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        page.items.size.toString(),
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text("Titles", style = MaterialTheme.typography.bodyMedium)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    featured.chunked(2).forEach { covers ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(HOME_GRID_GAP)) {
                                            covers.forEach { item ->
                                                MangaCover.Book(
                                                    data = item.manga.asMangaCover(),
                                                    modifier = Modifier.weight(1f),
                                                    contentDescription = item.manga.title,
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentScale = ContentScale.Crop,
                                                    onClick = { onClickManga(item.id, page.id, page.name) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (page.items.isEmpty()) {
                        item(key = "empty") {
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                Text("No titles here", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row {
                                    TextButton(onClick = onSearchSources) { Text("Search extensions") }
                                    TextButton(onClick = onGettingStarted) { Text("Getting started") }
                                }
                            }
                        }
                    }
                    items(rows, key = { it.first().id }) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(HOME_GRID_GAP),
                        ) {
                            row.forEach { item ->
                                EditorialMangaCard(
                                    item = item,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onClickManga(item.id, page.id, page.name) },
                                )
                            }
                            repeat(HOME_GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

private const val HOME_PREVIEW_SIZE = 8
private const val HOME_GRID_COLUMNS = 4
private val HOME_GRID_GAP = 8.dp

private fun DiscoveryCategorySection.navigationKey(): String =
    if (id < NO_GENRE_SECTION_ID) "genre:$name" else "section:$id"

@Composable
private fun LatestChaptersSummary(
    section: DiscoveryCategorySection,
    onClickManga: (LibraryManga) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current)
    val covers = rememberLazyListState()
    val preview = remember(section.items) { section.items.take(HOME_PREVIEW_SIZE) }
    LaunchedEffect(preview.firstOrNull()?.id) {
        covers.scrollToItem(0)
    }
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Latest chapters",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        if (preview.isNotEmpty()) Spacer(Modifier.height(16.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f, fill = false)) {
            val coverWidth = (maxWidth / 3.65f).coerceAtMost(118.dp).coerceAtMost(maxHeight * 2f / 3f)
            if (coverWidth >= 32.dp) {
                LazyRow(
                    state = covers,
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(preview, key = { it.id }) { item ->
                        MangaCover.Book(
                            data = item.manga.asMangaCover(),
                            modifier = Modifier.width(coverWidth),
                            contentDescription = item.manga.title,
                            shape = RoundedCornerShape(10.dp),
                            onClick = { onClickManga(item) },
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
        TextButton(onClick = onOpen, modifier = Modifier.padding(start = 12.dp), colors = buttonColors) {
            Text("View Latest Chapters")
        }
    }
}
