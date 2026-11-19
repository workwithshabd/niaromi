package eu.kanade.presentation.manga

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.icons.materialsymbols.roundedfilled.Bookmark
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import kotlin.math.absoluteValue

private val WarmIvoryBg = Color(0xFFFFFDF4)
private val NearBlackText = Color(0xFF171717)
private val MutedGrayText = Color(0xFF929292)
private val ButtonBlack = Color(0xFF1D1D1D)

@Composable
fun MangaEditorialDetailScreen(
    state: MangaViewModel.State.Success,
    navigateUp: () -> Unit,
    onContinueReading: () -> Unit,
    onChapterClicked: (Chapter) -> Unit,
    onAddToLibraryClicked: () -> Unit,
    onShareClicked: (() -> Unit)?,
    onWebViewClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onSelectManga: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val allFavorites by produceState(initialValue = emptyList<Manga>()) {
        value = context.appGraph.getFavorites.await()
    }

    val carouselMangaList = remember(allFavorites, state.manga) {
        val list = allFavorites.toMutableList()
        if (list.none { it.id == state.manga.id }) {
            list.add(0, state.manga)
        }
        list
    }

    val initialIndex = remember(carouselMangaList, state.manga) {
        carouselMangaList.indexOfFirst { it.id == state.manga.id }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { carouselMangaList.size },
    )

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val chaptersCache = remember { mutableMapOf<Long, List<Chapter>>() }

    LaunchedEffect(state.manga.id, state.processedChapters) {
        chaptersCache[state.manga.id] = state.processedChapters.map { it.chapter }
    }

    LaunchedEffect(pagerState, carouselMangaList) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page in carouselMangaList.indices) {
                val currentManga = carouselMangaList[page]
                if (currentManga.id != state.manga.id) {
                    onSelectManga(currentManga.id)
                }
            }
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        if (listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    var showMenu by remember { mutableStateOf(false) }

    val currentManga = carouselMangaList.getOrNull(pagerState.currentPage) ?: state.manga
    val activeManga = carouselMangaList.getOrNull(pagerState.settledPage) ?: state.manga
    val categoryHeading = currentManga.genre?.firstOrNull() ?: "MANGA DETAILS"

    val activeChapters by produceState(
        initialValue = chaptersCache[activeManga.id] ?: if (activeManga.id == state.manga.id) {
            state.processedChapters.map { it.chapter }
        } else {
            emptyList()
        },
        key1 = activeManga.id,
    ) {
        val cached = chaptersCache[activeManga.id]
        if (cached != null) {
            value = cached
        } else if (activeManga.id == state.manga.id) {
            val chapters = state.processedChapters.map { it.chapter }
            chaptersCache[activeManga.id] = chapters
            value = chapters
        } else {
            val chapters = context.appGraph.getChaptersByMangaId.await(activeManga.id)
            chaptersCache[activeManga.id] = chapters
            value = chapters
        }
    }

    val scrollToChapters: () -> Unit = {
        coroutineScope.launch {
            if (listState.firstVisibleItemIndex >= 1) {
                listState.animateScrollToItem(0)
            } else {
                listState.animateScrollToItem(1)
            }
        }
    }

    val hasReadAny = if (currentManga.id == state.manga.id) {
        state.hasReadChapters
    } else {
        activeChapters.any { it.read }
    }

    val nextChapter = remember(currentManga.id, activeChapters, state.processedChapters) {
        if (currentManga.id == state.manga.id) {
            null
        } else {
            activeChapters.firstOrNull { !it.read } ?: activeChapters.firstOrNull()
        }
    }

    val onReadButtonClick: () -> Unit = {
        if (currentManga.id == state.manga.id) {
            onContinueReading()
        } else if (nextChapter != null) {
            onChapterClicked(nextChapter)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmIvoryBg),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            DetailTopBar(
                categoryHeading = categoryHeading,
                onBack = navigateUp,
                onMenuClick = { showMenu = true },
                onTitleClick = scrollToChapters,
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 2,
                            key = { page -> carouselMangaList.getOrNull(page)?.id ?: page.toLong() },
                            contentPadding = PaddingValues(horizontal = 72.dp),
                            pageSpacing = 16.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(310.dp),
                        ) { page ->
                            val mangaItem = carouselMangaList.getOrNull(page) ?: return@HorizontalPager

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        val pageOffset =
                                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                        val fraction = (1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
                                        val animatedScale = 0.86f + (fraction * 0.14f)
                                        val animatedAlpha = 0.45f + (fraction * 0.55f)
                                        scaleX = animatedScale
                                        scaleY = animatedScale
                                        alpha = animatedAlpha
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(200.dp)
                                        .height(295.dp)
                                        .shadow(
                                            elevation = 8.dp,
                                            shape = RoundedCornerShape(8.dp),
                                            ambientColor = Color(0x30000000),
                                            spotColor = Color(0x40000000),
                                        )
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEDE9DF))
                                        .clickable {
                                            if (pagerState.currentPage != page) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(page)
                                                }
                                            }
                                        },
                                ) {
                                    MangaCover.Book(
                                        data = mangaItem.asMangaCover(),
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                        ) {
                            Text(
                                text = currentManga.title,
                                style = TextStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = NearBlackText,
                                    textAlign = TextAlign.Center,
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = scrollToChapters,
                                ),
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val author =
                                currentManga.author?.ifBlank { null } ?: currentManga.artist ?: "Unknown Author"
                            Text(
                                text = author,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = MutedGrayText,
                                    textAlign = TextAlign.Center,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val chapterCount = activeChapters.size
                            val statusText = when (currentManga.status.toInt()) {
                                1 -> "Ongoing"
                                2 -> "Completed"
                                3 -> "Licensed"
                                4 -> "Publishing Finished"
                                5 -> "Cancelled"
                                6 -> "On Hiatus"
                                else -> "Manga"
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = scrollToChapters,
                                ),
                            ) {
                                Text(
                                    text = "$chapterCount Chapters",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        color = MutedGrayText,
                                    ),
                                )
                                Text(
                                    text = statusText,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        color = MutedGrayText,
                                    ),
                                )
                                Text(
                                    text = "★ 4.6",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        color = MutedGrayText,
                                    ),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Surface(
                            onClick = onReadButtonClick,
                            shape = CircleShape,
                            color = ButtonBlack,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .widthIn(min = 125.dp, max = 150.dp)
                                .height(46.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                Text(
                                    text = if (hasReadAny) "Continue Reading" else "Read Now",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Chapters (${activeChapters.size})",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = NearBlackText,
                                ),
                            )
                            if (activeChapters.any { it.read }) {
                                val readCount = activeChapters.count { it.read }
                                Text(
                                    text = "$readCount / ${activeChapters.size} read",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        color = MutedGrayText,
                                    ),
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color(0x14000000),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        )
                    }
                }

                if (activeChapters.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No chapters available",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    color = MutedGrayText,
                                ),
                            )
                        }
                    }
                } else {
                    items(
                        items = activeChapters,
                        key = { it.id },
                    ) { chapter ->
                        MangaEditorialChapterRow(
                            chapter = chapter,
                            onClick = { onChapterClicked(chapter) },
                        )
                        HorizontalDivider(
                            color = Color(0x0C000000),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }

                item {
                    Spacer(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .height(36.dp),
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(if (currentManga.favorite) "In library" else "Add to library") },
                onClick = {
                    showMenu = false
                    onAddToLibraryClicked()
                },
            )
            DropdownMenuItem(
                text = { Text("View all chapters") },
                onClick = {
                    showMenu = false
                    scrollToChapters()
                },
            )
            if (onShareClicked != null) {
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = {
                        showMenu = false
                        onShareClicked()
                    },
                )
            }
            if (onWebViewClicked != null) {
                DropdownMenuItem(
                    text = { Text("Open in WebView") },
                    onClick = {
                        showMenu = false
                        onWebViewClicked()
                    },
                )
            }
            if (onEditCategoryClicked != null) {
                DropdownMenuItem(
                    text = { Text("Edit category") },
                    onClick = {
                        showMenu = false
                        onEditCategoryClicked()
                    },
                )
            }
            if (onMigrateClicked != null) {
                DropdownMenuItem(
                    text = { Text("Migrate") },
                    onClick = {
                        showMenu = false
                        onMigrateClicked()
                    },
                )
            }
        }
    }
}

@Composable
fun MangaEditorialChapterRow(
    chapter: Chapter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateText = relativeDateText(chapter.dateUpload)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
        ) {
            Text(
                text = chapter.name,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = if (chapter.read) MutedGrayText else NearBlackText,
                    fontWeight = if (chapter.read) FontWeight.Normal else FontWeight.Medium,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (dateText.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateText,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 11.sp,
                        color = MutedGrayText,
                    ),
                    maxLines = 1,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (chapter.bookmark) {
                Icon(
                    imageVector = mihon.icons.materialsymbols.MaterialSymbols.RoundedFilled.Bookmark,
                    contentDescription = "Bookmarked",
                    tint = NearBlackText,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (chapter.read) {
                Text(
                    text = "Read",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = MutedGrayText,
                    ),
                )
            }
        }
    }
}

@Composable
fun DetailTopBar(
    categoryHeading: String,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = mihon.icons.materialsymbols.MaterialSymbols.AutoMirroredRounded.ArrowBack,
                    contentDescription = "Back",
                    tint = NearBlackText,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTitleClick,
                    ),
            ) {
                Text(
                    text = "POPULAR MANGA OF",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        color = MutedGrayText,
                    ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = categoryHeading,
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = NearBlackText,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = mihon.icons.materialsymbols.MaterialSymbols.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = NearBlackText,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
