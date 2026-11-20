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
import androidx.compose.material3.MaterialTheme
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
import niaromi.icons.materialsymbols.MaterialSymbols
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
import niaromi.presentation.category.components.ChangeCategoryDialog
import niaromi.presentation.components.relativeDateText
import niaromi.presentation.core.util.collectAsState
import niaromi.presentation.manga.DownloadAction
import niaromi.presentation.manga.components.ChapterDownloadAction
import niaromi.presentation.manga.components.MangaCover
import niaromi.ui.manga.ChapterList
import niaromi.ui.manga.MangaViewModel
import niaromi.util.system.toast
import kotlin.math.absoluteValue

private val DetailBg: Color
    @Composable get() = MaterialTheme.colorScheme.background

private val DetailText: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

private val DetailMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val ButtonPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.primary

@Composable
fun MangaEditorialDetailScreen(
    state: MangaViewModel.State.Success,
    fromSearch: Boolean = false,
    categoryName: String? = null,
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
    val libraryMangaList by produceState(initialValue = emptyList<niaromi.domain.library.model.LibraryManga>()) {
        value = context.appGraph.getLibraryManga.await()
    }
    val allFavorites = remember(libraryMangaList) { libraryMangaList.map { it.manga } }

    val initialManga = remember { state.manga }

    val resolvedCategoryName = remember(initialManga, categoryName) {
        if (!categoryName.isNullOrBlank()) {
            categoryName.trim()
        } else {
            initialManga.genre?.firstOrNull { it.isNotBlank() }
                ?.trim()
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                ?: ""
        }
    }

    val (carouselMangaList, initialPageIndex) = remember(
        allFavorites,
        libraryMangaList,
        initialManga,
        resolvedCategoryName,
        fromSearch,
    ) {
        if (fromSearch) {
            listOf(initialManga) to 0
        } else {
            val sameGenreFavorites = if (resolvedCategoryName.isNotBlank()) {
                if (resolvedCategoryName.equals("Latest Chapters", ignoreCase = true)) {
                    libraryMangaList.filter { it.hasLatestChapters }
                        .sortedWith(
                            compareByDescending<niaromi.domain.library.model.LibraryManga> {
                                it.chapterFetchedAt?.toEpochMilliseconds()
                                    ?: 0L
                            }
                                .thenByDescending { it.manga.lastUpdate?.toEpochMilliseconds() ?: 0L }
                                .thenByDescending { it.latestUpload?.toEpochMilliseconds() ?: 0L }
                                .thenByDescending { it.manga.favoriteAt?.toEpochMilliseconds() ?: 0L },
                        )
                        .map { it.manga }
                } else {
                    allFavorites.filter { fav ->
                        val favPrimary = fav.genre?.firstOrNull { it.isNotBlank() }?.trim()
                        favPrimary?.equals(resolvedCategoryName, ignoreCase = true) == true ||
                            fav.genre?.any { it.trim().equals(resolvedCategoryName, ignoreCase = true) } == true
                    }
                }
            } else {
                emptyList()
            }

            val sortedList = if (sameGenreFavorites.isNotEmpty()) {
                if (resolvedCategoryName.equals("Latest Chapters", ignoreCase = true)) {
                    sameGenreFavorites
                } else {
                    sameGenreFavorites.sortedWith(
                        compareByDescending<Manga> { it.favoriteAt?.toEpochMilliseconds() ?: 0L }
                            .thenByDescending { it.lastUpdate?.toEpochMilliseconds() ?: 0L },
                    )
                }
            } else {
                listOf(initialManga)
            }

            val targetIndex = sortedList.indexOfFirst { it.id == initialManga.id }
            if (targetIndex >= 0) {
                // Slice up to 5 previous and up to 5 next titles around target
                val start = maxOf(0, targetIndex - 5)
                val end = minOf(sortedList.size, targetIndex + 6)
                val slice = sortedList.subList(start, end)
                val pageIndex = targetIndex - start
                slice to pageIndex
            } else {
                val remaining = sortedList.filter { it.id != initialManga.id }.take(5)
                (listOf(initialManga) + remaining) to 0
            }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { carouselMangaList.size },
    )

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val chaptersCache = remember { mutableMapOf<Long, List<Chapter>>() }

    LaunchedEffect(state.manga.id, state.processedChapters) {
        chaptersCache[state.manga.id] = state.processedChapters.map { it.chapter }
    }

    LaunchedEffect(pagerState.settledPage) {
        if (listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showChangeCategoryDialogForCurrent by remember { mutableStateOf(false) }
    var selectedChapterForMenu by remember { mutableStateOf<Chapter?>(null) }
    val downloadPreferences = remember { context.appGraph.downloadPreferences }
    val showChapterDownloadButtons by downloadPreferences.showChapterDownloadButtons.collectAsState()

    val currentManga = carouselMangaList.getOrNull(pagerState.currentPage) ?: state.manga
    val activeManga = carouselMangaList.getOrNull(pagerState.settledPage) ?: state.manga
    val categoryHeading = resolvedCategoryName.ifBlank {
        currentManga.genre?.firstOrNull() ?: "MANGA DETAILS"
    }

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
            .background(DetailBg),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            DetailTopBar(
                categoryHeading = categoryHeading,
                onBack = navigateUp,
                onMenuClick = { showMenu = true },
                onTitleClick = scrollToChapters,
                menu = {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Info") },
                            leadingIcon = {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.Info,
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
                                text = { Text("Change genre / collection") },
                                onClick = {
                                    showMenu = false
                                    if (currentManga.id == state.manga.id) {
                                        onEditCategoryClicked()
                                    } else {
                                        showChangeCategoryDialogForCurrent = true
                                    }
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
                },
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
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
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
                                    color = DetailText,
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
                                        color = DetailMuted,
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
                            if (currentManga.favorite) {
                                // Once in library: strictly 2 options (Continue & Download)
                                Surface(
                                    onClick = onReadButtonClick,
                                    shape = CircleShape,
                                    color = ButtonPrimary,
                                    shadowElevation = 3.dp,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .height(44.dp),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 22.dp),
                                    ) {
                                        Text(
                                            text = "Continue",
                                            style = TextStyle(
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onPrimary,
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
                                            coroutineScope.launch {
                                                context.appGraph.downloadManager.downloadChapters(
                                                    currentManga,
                                                    activeChapters,
                                                )
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .height(44.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    ) {
                                        Icon(
                                            imageVector = MaterialSymbols.Rounded.Download,
                                            contentDescription = "Download All Chapters",
                                            tint = DetailText,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Download",
                                            style = TextStyle(
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = DetailText,
                                            ),
                                        )
                                    }
                                }
                            } else {
                                // Not in library: Read Now / Continue, Add to Library (no download button)
                                Surface(
                                    onClick = onReadButtonClick,
                                    shape = CircleShape,
                                    color = ButtonPrimary,
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
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                }

                                Surface(
                                    onClick = onAddToLibraryClicked,
                                    shape = CircleShape,
                                    color = ButtonPrimary,
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
                                            imageVector = MaterialSymbols.Rounded.BookmarkAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Add to Library",
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
                                    color = DetailText,
                                ),
                            )
                            if (activeChapters.any { it.read }) {
                                val readCount = activeChapters.count { it.read }
                                Text(
                                    text = "$readCount / ${activeChapters.size} read",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        color = DetailMuted,
                                    ),
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
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
                                    color = DetailMuted,
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
                                } else {
                                    coroutineScope.launch {
                                        context.appGraph.downloadManager.downloadChapters(currentManga, listOf(chapter))
                                    }
                                }
                            },
                            onDeleteClick = {
                                onMultiDeleteClicked?.invoke(listOf(chapter))
                            },
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
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

        if (showChangeCategoryDialogForCurrent) {
            ChangeCategoryDialog(
                initialSelection = emptyList(),
                onDismissRequest = { showChangeCategoryDialogForCurrent = false },
                onEditCategories = {},
                onConfirm = { include, _ ->
                    coroutineScope.launch {
                        context.appGraph.setMangaCategories.await(currentManga.id, include)
                    }
                },
                manga = currentManga,
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
                    color = if (chapter.read) DetailMuted else DetailText,
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
                        color = DetailMuted,
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
                    imageVector = MaterialSymbols.RoundedFilled.Bookmark,
                    contentDescription = "Bookmarked",
                    tint = DetailText,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (chapter.read) {
                Text(
                    text = "Read",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = DetailMuted,
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
                            imageVector = MaterialSymbols.RoundedFilled.CheckCircle,
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
                            imageVector = MaterialSymbols.Rounded.Download,
                            contentDescription = "Download chapter",
                            tint = DetailText,
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
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = chapter.name,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DetailText,
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
                        icon = MaterialSymbols.Rounded.Check,
                        onClick = onMarkAsRead,
                    )
                } else {
                    ChapterActionRow(
                        title = "Mark as unread",
                        icon = MaterialSymbols.Rounded.Close,
                        onClick = onMarkAsUnread,
                    )
                }

                ChapterActionRow(
                    title = "Mark chapters below as read",
                    icon = MaterialSymbols.Rounded.DoneAll,
                    onClick = onMarkChaptersBelowAsRead,
                )

                if (isDownloaded) {
                    ChapterActionRow(
                        title = "Delete download",
                        icon = MaterialSymbols.Rounded.Delete,
                        onClick = onDeleteDownload,
                    )
                } else {
                    ChapterActionRow(
                        title = "Download",
                        icon = MaterialSymbols.Rounded.Download,
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
                        color = DetailMuted,
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
            tint = DetailText,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DetailText,
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
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = DetailText,
        textContentColor = DetailText,
        title = {
            Text(
                text = manga.title,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DetailText,
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
                            color = DetailText,
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
                            color = DetailMuted,
                        ),
                    )
                    Text(
                        text = "•",
                        style = TextStyle(color = DetailMuted),
                    )
                    Text(
                        text = "$chapterCount Chapters",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            color = DetailMuted,
                        ),
                    )
                    Text(
                        text = "•",
                        style = TextStyle(color = DetailMuted),
                    )
                    Text(
                        text = "★ 4.6",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            color = DetailMuted,
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
                            color = DetailMuted,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                val description = manga.description?.ifBlank { null } ?: "No description available."
                Text(
                    text = description,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = DetailText,
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
                        color = DetailText,
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
    menu: @Composable () -> Unit = {},
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
                    imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                    contentDescription = "Back",
                    tint = DetailText,
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
                        color = DetailMuted,
                    ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = categoryHeading,
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = DetailText,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.MoreVert,
                        contentDescription = "More",
                        tint = DetailText,
                        modifier = Modifier.size(22.dp),
                    )
                }
                menu()
            }
        }
    }
}
