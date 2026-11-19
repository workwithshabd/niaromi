package niaromi.presentation.manga

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import niaromi.app.di.appGraph
import niaromi.domain.chapter.model.Chapter
import niaromi.domain.manga.model.Manga
import niaromi.domain.manga.model.asMangaCover
import niaromi.icons.materialsymbols.automirroredrounded.ArrowBack
import niaromi.icons.materialsymbols.rounded.BookmarkAdd
import niaromi.icons.materialsymbols.rounded.Check
import niaromi.icons.materialsymbols.rounded.Close
import niaromi.icons.materialsymbols.rounded.Delete
import niaromi.icons.materialsymbols.rounded.DoneAll
import niaromi.icons.materialsymbols.rounded.Download
import niaromi.icons.materialsymbols.rounded.Info
import niaromi.icons.materialsymbols.rounded.MoreVert
import niaromi.icons.materialsymbols.roundedfilled.Bookmark
import niaromi.icons.materialsymbols.roundedfilled.CheckCircle
import niaromi.presentation.components.relativeDateText
import niaromi.presentation.core.util.collectAsState
import niaromi.presentation.manga.DownloadAction
import niaromi.presentation.manga.components.ChapterDownloadAction
import niaromi.presentation.manga.components.MangaCover
import niaromi.ui.manga.ChapterList
import niaromi.ui.manga.MangaViewModel
import niaromi.util.system.toast
import kotlin.math.absoluteValue

private val WarmIvoryBg = Color(0xFFFFFDF4)
private val NearBlackText = Color(0xFF171717)
private val MutedGrayText = Color(0xFF929292)
private val ButtonBlack = Color(0xFF1D1D1D)

@Composable
fun MangaEditorialDetailScreen(
    state: MangaViewModel.State.Success,
    fromSearch: Boolean = false,
    navigateUp: () -> Unit,
    onContinueReading: () -> Unit,
    onChapterClicked: (Chapter) -> Unit,
    onAddToLibraryClicked: () -> Unit,
    onShareClicked: (() -> Unit)?,
    onWebViewClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)? = null,
    onDownloadChapter: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)? = null,
    onMultiMarkAsReadClicked: ((List<Chapter>, Boolean) -> Unit)? = null,
    onMarkPreviousAsReadClicked: ((Chapter) -> Unit)? = null,
    onMultiDeleteClicked: ((List<Chapter>) -> Unit)? = null,
    onTagSearch: ((String) -> Unit)? = null,
    onCoverClicked: (() -> Unit)? = null,
    onSelectManga: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val allFavorites by produceState(initialValue = emptyList<Manga>()) {
        value = context.appGraph.getFavorites.await()
    }

    val currentGenres = remember(state.manga) {
        state.manga.genre?.map { it.trim().lowercase() }?.toSet().orEmpty()
    }

    val carouselMangaList = remember(allFavorites, state.manga, currentGenres, fromSearch) {
        if (fromSearch) {
            listOf(state.manga)
        } else {
            val otherFavorites = allFavorites.filter { it.id != state.manga.id }
            val (sameGenre, differentGenre) = otherFavorites.partition { fav ->
                fav.genre?.any { it.trim().lowercase() in currentGenres } == true
            }
            // state.manga is ALWAYS at index 0! Same-genre titles come immediately after!
            listOf(state.manga) + sameGenre + differentGenre
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
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
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedChapterForMenu by remember { mutableStateOf<Chapter?>(null) }
    val downloadPreferences = remember { context.appGraph.downloadPreferences }
    val showChapterDownloadButtons by downloadPreferences.showChapterDownloadButtons.collectAsState()

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
        key2 = state.processedChapters,
    ) {
        if (activeManga.id == state.manga.id && state.processedChapters.isNotEmpty()) {
            val chapters = state.processedChapters.map { it.chapter }
            chaptersCache[activeManga.id] = chapters
            value = chapters
        } else {
            val cached = chaptersCache[activeManga.id]
            if (cached != null && cached.isNotEmpty()) {
                value = cached
            } else {
                val chapters = context.appGraph.getChaptersByMangaId.await(activeManga.id)
                chaptersCache[activeManga.id] = chapters
                value = chapters
            }
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

    val nextChapter = remember(activeManga.id, activeChapters, state.processedChapters) {
        if (activeManga.id == state.manga.id) {
            state.processedChapters.firstOrNull { !it.chapter.read }?.chapter
                ?: state.processedChapters.firstOrNull()?.chapter
                ?: activeChapters.firstOrNull { !it.read }
                ?: activeChapters.firstOrNull()
        } else {
            activeChapters.firstOrNull { !it.read } ?: activeChapters.firstOrNull()
        }
    }

    val onReadButtonClick: () -> Unit = {
        if (currentManga.id == state.manga.id) {
            onContinueReading()
        } else if (nextChapter != null && nextChapter.id > 0) {
            onChapterClicked(nextChapter)
        } else {
            onSelectManga(currentManga.id)
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
                            userScrollEnabled = !fromSearch && carouselMangaList.size > 1,
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

                            val author = remember(currentManga, allFavorites) {
                                currentManga.author?.ifBlank { null }
                                    ?: currentManga.artist?.ifBlank { null }
                                    ?: allFavorites.firstOrNull {
                                        it.title.equals(currentManga.title, ignoreCase = true) &&
                                            !it.author.isNullOrBlank()
                                    }?.author
                            }

                            if (!author.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
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
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                onClick = onReadButtonClick,
                                shape = CircleShape,
                                color = ButtonBlack,
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .height(44.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 18.dp),
                                ) {
                                    Text(
                                        text = if (hasReadAny) "Continue" else "Read Now",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    if (currentManga.id == state.manga.id) {
                                        if (onDownloadActionClicked != null) {
                                            onDownloadActionClicked(DownloadAction.ALL_CHAPTERS)
                                        } else if (onDownloadChapter != null) {
                                            val items = state.processedChapters.filter { !it.isDownloaded }
                                            if (items.isNotEmpty()) {
                                                onDownloadChapter(items, ChapterDownloadAction.START)
                                            }
                                        }
                                    } else {
                                        onSelectManga(currentManga.id)
                                    }
                                },
                                shape = CircleShape,
                                color = Color(0xFFF3EEDF),
                                border = BorderStroke(1.dp, Color(0xFFD6CDBA)),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .height(44.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                ) {
                                    Icon(
                                        imageVector = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Download,
                                        contentDescription = "Download All Chapters",
                                        tint = NearBlackText,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Download",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = NearBlackText,
                                        ),
                                    )
                                }
                            }

                            Surface(
                                onClick = onAddToLibraryClicked,
                                shape = CircleShape,
                                color = if (currentManga.favorite) Color(0xFFF3EEDF) else ButtonBlack,
                                border = if (currentManga.favorite) BorderStroke(1.dp, Color(0xFFD6CDBA)) else null,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .height(44.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                ) {
                                    Icon(
                                        imageVector = if (currentManga.favorite) {
                                            niaromi.icons.materialsymbols.MaterialSymbols.RoundedFilled.Bookmark
                                        } else {
                                            niaromi.icons.materialsymbols.MaterialSymbols.Rounded.BookmarkAdd
                                        },
                                        contentDescription = null,
                                        tint = if (currentManga.favorite) NearBlackText else Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (currentManga.favorite) "In Library" else "Add to Library",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (currentManga.favorite) NearBlackText else Color.White,
                                        ),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
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
                        val chapterItem = remember(chapter.id, state.processedChapters) {
                            state.processedChapters.find { it.chapter.id == chapter.id }
                        }
                        val isDownloaded = chapterItem?.isDownloaded == true
                        MangaEditorialChapterRow(
                            chapter = chapter,
                            onClick = {
                                if (chapter.id > 0) {
                                    onChapterClicked(chapter)
                                } else {
                                    onSelectManga(currentManga.id)
                                }
                            },
                            onLongClick = {
                                selectedChapterForMenu = chapter
                            },
                            showDownloadButton = showChapterDownloadButtons,
                            isDownloaded = isDownloaded,
                            onDownloadClick = {
                                if (chapterItem != null && onDownloadChapter != null) {
                                    onDownloadChapter(listOf(chapterItem), ChapterDownloadAction.START)
                                }
                            },
                            onDeleteClick = {
                                onMultiDeleteClicked?.invoke(listOf(chapter))
                            },
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
                text = { Text("Info") },
                leadingIcon = {
                    Icon(
                        imageVector = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    showMenu = false
                    showInfoDialog = true
                },
            )
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

        if (showInfoDialog) {
            MangaInfoDialog(
                manga = currentManga,
                chapterCount = activeChapters.size,
                onDismissRequest = { showInfoDialog = false },
            )
        }

        if (selectedChapterForMenu != null) {
            val targetChapter = selectedChapterForMenu!!
            val targetItem = remember(targetChapter.id, state.processedChapters) {
                state.processedChapters.find { it.chapter.id == targetChapter.id }
            }
            val isDownloaded = targetItem?.isDownloaded == true

            ChapterActionMenuDialog(
                chapter = targetChapter,
                isDownloaded = isDownloaded,
                onDismissRequest = { selectedChapterForMenu = null },
                onMarkAsRead = {
                    onMultiMarkAsReadClicked?.invoke(listOf(targetChapter), true)
                    selectedChapterForMenu = null
                },
                onMarkAsUnread = {
                    onMultiMarkAsReadClicked?.invoke(listOf(targetChapter), false)
                    selectedChapterForMenu = null
                },
                onMarkChaptersBelowAsRead = {
                    onMarkPreviousAsReadClicked?.invoke(targetChapter)
                    selectedChapterForMenu = null
                },
                onDeleteDownload = {
                    onMultiDeleteClicked?.invoke(listOf(targetChapter))
                    selectedChapterForMenu = null
                },
                onDownload = {
                    if (targetItem != null && onDownloadChapter != null) {
                        onDownloadChapter(listOf(targetItem), ChapterDownloadAction.START)
                    }
                    selectedChapterForMenu = null
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaEditorialChapterRow(
    chapter: Chapter,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    showDownloadButton: Boolean = false,
    isDownloaded: Boolean = false,
    onDownloadClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val dateText = relativeDateText(chapter.dateUpload)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
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
                    imageVector = niaromi.icons.materialsymbols.MaterialSymbols.RoundedFilled.Bookmark,
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
            if (showDownloadButton) {
                if (isDownloaded) {
                    IconButton(
                        onClick = { onDeleteClick?.invoke() },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = niaromi.icons.materialsymbols.MaterialSymbols.RoundedFilled.CheckCircle,
                            contentDescription = "Downloaded - Click to delete",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    IconButton(
                        onClick = { onDownloadClick?.invoke() },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Download,
                            contentDescription = "Download chapter",
                            tint = NearBlackText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterActionMenuDialog(
    chapter: Chapter,
    isDownloaded: Boolean,
    onDismissRequest: () -> Unit,
    onMarkAsRead: () -> Unit,
    onMarkAsUnread: () -> Unit,
    onMarkChaptersBelowAsRead: () -> Unit,
    onDeleteDownload: () -> Unit,
    onDownload: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(20.dp),
        containerColor = WarmIvoryBg,
        title = {
            Text(
                text = chapter.name,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NearBlackText,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!chapter.read) {
                    ChapterActionRow(
                        title = "Mark as read",
                        icon = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Check,
                        onClick = onMarkAsRead,
                    )
                } else {
                    ChapterActionRow(
                        title = "Mark as unread",
                        icon = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Close,
                        onClick = onMarkAsUnread,
                    )
                }

                ChapterActionRow(
                    title = "Mark chapters below as read",
                    icon = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.DoneAll,
                    onClick = onMarkChaptersBelowAsRead,
                )

                if (isDownloaded) {
                    ChapterActionRow(
                        title = "Delete download",
                        icon = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Delete,
                        onClick = onDeleteDownload,
                    )
                } else {
                    ChapterActionRow(
                        title = "Download",
                        icon = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Download,
                        onClick = onDownload,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "Cancel",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        color = MutedGrayText,
                    ),
                )
            }
        },
    )
}

@Composable
private fun ChapterActionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NearBlackText,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = NearBlackText,
            ),
        )
    }
}

@Composable
fun MangaInfoDialog(
    manga: Manga,
    chapterCount: Int,
    onDismissRequest: () -> Unit,
) {
    val statusText = when (manga.status.toInt()) {
        1 -> "Ongoing"
        2 -> "Completed"
        3 -> "Licensed"
        4 -> "Publishing Finished"
        5 -> "Cancelled"
        6 -> "On Hiatus"
        else -> "Unknown"
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(20.dp),
        containerColor = WarmIvoryBg,
        titleContentColor = NearBlackText,
        textContentColor = NearBlackText,
        title = {
            Text(
                text = manga.title,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = NearBlackText,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                val author = manga.author?.ifBlank { null } ?: manga.artist?.ifBlank { null }
                if (!author.isNullOrBlank()) {
                    Text(
                        text = "Author: $author",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NearBlackText,
                        ),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Status: $statusText",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            color = MutedGrayText,
                        ),
                    )
                    Text(
                        text = "•",
                        style = TextStyle(color = MutedGrayText),
                    )
                    Text(
                        text = "$chapterCount Chapters",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            color = MutedGrayText,
                        ),
                    )
                    Text(
                        text = "•",
                        style = TextStyle(color = MutedGrayText),
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

                if (!manga.genre.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Genres: " + manga.genre!!.joinToString(", "),
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            color = MutedGrayText,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0x14000000), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                val description = manga.description?.ifBlank { null } ?: "No description available."
                Text(
                    text = description,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = NearBlackText,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "Close",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = NearBlackText,
                    ),
                )
            }
        },
    )
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
                    imageVector = niaromi.icons.materialsymbols.MaterialSymbols.AutoMirroredRounded.ArrowBack,
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
                    imageVector = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = NearBlackText,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
