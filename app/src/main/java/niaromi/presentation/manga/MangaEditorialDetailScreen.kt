package niaromi.presentation.manga

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import niaromi.app.di.appGraph
import niaromi.core.common.preference.CheckboxState
import niaromi.core.navigation.CategoryRoute
import niaromi.core.navigation.LocalNavigator
import niaromi.domain.category.model.Category
import niaromi.domain.chapter.model.Chapter
import niaromi.domain.chapter.service.latestChapterFirst
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
import niaromi.icons.materialsymbols.rounded.FilterList
import niaromi.icons.materialsymbols.rounded.Info
import niaromi.icons.materialsymbols.rounded.Sync
import niaromi.icons.materialsymbols.roundedfilled.Bookmark
import niaromi.icons.materialsymbols.roundedfilled.CheckCircle
import niaromi.presentation.category.components.ChangeCategoryDialog
import niaromi.presentation.components.relativeDateText
import niaromi.presentation.core.components.material.PullRefresh
import niaromi.presentation.core.components.material.refreshStretch
import niaromi.presentation.core.components.material.rememberRefreshPull
import niaromi.presentation.core.components.material.rememberSpringRefreshState
import niaromi.presentation.core.screens.LoadingScreen
import niaromi.presentation.core.util.collectAsState
import niaromi.presentation.library.components.DiscoveryCategorySection
import niaromi.presentation.library.components.NO_GENRE_SECTION_ID
import niaromi.presentation.library.components.buildDiscoverySections
import niaromi.presentation.library.components.sortDiscoveryTitles
import niaromi.presentation.manga.DownloadAction
import niaromi.presentation.manga.components.ChapterDownloadAction
import niaromi.presentation.manga.components.MangaCover
import niaromi.ui.manga.ChapterList
import niaromi.ui.manga.MangaViewModel
import niaromi.util.chapter.chapterToContinue
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

private data class CarouselPage(val section: DiscoveryCategorySection?, val manga: Manga)

@Composable
fun MangaEditorialDetailScreen(
    state: MangaViewModel.State.Success,
    fromSearch: Boolean = false,
    categoryName: String? = null,
    sectionId: Long? = null,
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
    onRefresh: () -> Unit = {},
    onMigrateChapter: ((Chapter, Long, String, String, String, String?) -> Unit)? = null,
    searchChapterInOtherSources: (suspend (Manga, Chapter) -> List<MangaViewModel.ChapterMigrationCandidate>)? = null,
    onSelectManga: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val initialManga = remember { state.manga }
    val graph = context.appGraph
    val carousel by produceState<List<CarouselPage>?>(null, initialManga.id, sectionId, categoryName, fromSearch) {
        if (fromSearch || !initialManga.favorite || (sectionId == null && initialManga.primaryGenre == null)) {
            value = listOf(CarouselPage(null, initialManga))
            return@produceState
        }
        if (sectionId == 0L) {
            val titles = withContext(Dispatchers.IO) {
                sortDiscoveryTitles(
                    graph.getLibraryManga.cached ?: graph.getLibraryManga.await(),
                    graph.libraryPreferences.discoverySort.get(),
                    0L,
                )
            }
            value = if (titles.any { it.id == initialManga.id }) {
                titles.map { CarouselPage(null, it.manga) }
            } else {
                listOf(CarouselPage(null, initialManga))
            }
            return@produceState
        }
        val sections = withContext(Dispatchers.IO) {
            buildDiscoverySections(
                graph.getCategories.await(),
                graph.getLibraryManga.await(),
                graph.libraryPreferences.discoverySort.get(),
            )
        }
        val start = sections.indexOfFirst { it.matches(sectionId, categoryName ?: initialManga.primaryGenre) }
        if (start < 0) {
            value = listOf(CarouselPage(null, initialManga))
            return@produceState
        }
        val titles = sections.drop(start).flatMap { section -> section.items.map { CarouselPage(section, it.manga) } }
            .distinctBy { it.manga.id }
        value = if (titles.any { it.manga.id == initialManga.id }) titles else listOf(CarouselPage(null, initialManga))
    }
    val pages = carousel ?: run {
        LoadingScreen(modifier)
        return
    }
    val carouselMangaList = remember(pages) { pages.map { it.manga } }
    val initialPageIndex = remember(pages, initialManga.id) { pages.indexOfFirst { it.manga.id == initialManga.id } }

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { carouselMangaList.size },
    )

    LaunchedEffect(pages, pagerState.currentPage) {
        if (pagerState.currentPage !in pages.indices) {
            pagerState.scrollToPage(initialPageIndex.coerceIn(pages.indices))
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.settledPage) {
        if (listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showChangeCategoryDialogForCurrent by remember { mutableStateOf(false) }
    var selectedChapterForMenu by remember { mutableStateOf<Chapter?>(null) }
    var chapterToMigrate by remember { mutableStateOf<Chapter?>(null) }
    val downloadPreferences = remember { context.appGraph.downloadPreferences }
    val showChapterDownloadButtons by downloadPreferences.showChapterDownloadButtons.collectAsState()

    val currentPageManga = carouselMangaList.getOrNull(pagerState.currentPage) ?: state.manga
    val mangaFlow = remember(context, currentPageManga.id) {
        context.appGraph.getManga.subscribe(currentPageManga.id)
    }
    val observedManga by mangaFlow.collectAsStateWithLifecycle(initialValue = currentPageManga)
    val currentManga = when (currentPageManga.id) {
        state.manga.id -> state.manga
        observedManga.id -> observedManga
        else -> currentPageManga
    }
    val activeManga = carouselMangaList.getOrNull(pagerState.settledPage) ?: state.manga
    val currentSection = pages.getOrNull(pagerState.currentPage)?.section
    val categoryHeading = currentSection?.takeUnless { it.id < NO_GENRE_SECTION_ID }?.name
        ?: currentManga.primaryGenre?.replaceFirstChar { it.titlecase() }
        ?: currentManga.title

    var titleY by remember { mutableFloatStateOf(Float.POSITIVE_INFINITY) }
    var toolbarBottom by remember { mutableFloatStateOf(0f) }
    val showingChapters by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || titleY <= toolbarBottom }
    }
    val refreshState = rememberSpringRefreshState()
    val pull = rememberRefreshPull(refreshState, state.isRefreshingData)

    val prefetched = remember { mutableStateMapOf<Long, List<Chapter>>() }
    val prefetchCenter = pagerState.targetPage
    LaunchedEffect(pages, prefetchCenter) {
        val nearby = (-3..3).mapNotNull { offset -> pages.getOrNull(prefetchCenter + offset)?.manga?.id }
        val missing = nearby.filter { it != state.manga.id && it !in prefetched }
        if (missing.isNotEmpty()) {
            val chapters = withContext(Dispatchers.IO) {
                graph.getChaptersByMangaId.await(missing).mapValues { (_, items) ->
                    items.sortedWith(latestChapterFirst)
                }
            }
            missing.forEach { prefetched[it] = chapters[it].orEmpty() }
        }
        val retained = (nearby + activeManga.id).toSet()
        prefetched.keys.toList().filterNot { it in retained }.forEach(prefetched::remove)
    }

    val activeChapters = if (activeManga.id == state.manga.id) {
        remember(state.processedChapters) { state.processedChapters.map { it.chapter } }
    } else {
        prefetched[activeManga.id].orEmpty()
    }

    val chaptersReady = activeManga.id == state.manga.id || activeManga.id in prefetched
    val storedSummary = remember(activeManga.id, chaptersReady) {
        if (chaptersReady) null else graph.getLibraryManga.cached?.firstOrNull { it.id == activeManga.id }
    }
    val chapterCount = storedSummary?.totalChapters ?: activeChapters.size.toLong()
    val readCount = storedSummary?.readCount ?: activeChapters.count { it.read }.toLong()

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

    val onReadButtonClick: () -> Unit = {
        if (currentManga.id == state.manga.id) {
            onContinueReading()
        } else {
            val targetId = currentManga.id
            coroutineScope.launch {
                val chapter = withContext(Dispatchers.IO) {
                    graph.getChaptersByMangaId.await(targetId).chapterToContinue(graph.getHistory.await(targetId))
                }
                if (chapter != null) onChapterClicked(chapter) else onSelectManga(targetId)
            }
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
                categoryHeading = if (showingChapters) currentManga.title else categoryHeading,
                modifier = Modifier.onGloballyPositioned { toolbarBottom = it.positionInRoot().y + it.size.height },
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

            PullRefresh(
                refreshing = state.isRefreshingData,
                state = refreshState,
                enabled = true,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().refreshStretch(pull),
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
                                beyondViewportPageCount = 0,
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
                                        val coverManga = currentManga.takeIf { it.id == mangaItem.id } ?: mangaItem
                                        MangaCover.Book(
                                            data = coverManga.asMangaCover(),
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
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = DetailText,
                                    minLines = 2,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .onGloballyPositioned { titleY = it.positionInRoot().y }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = scrollToChapters,
                                        ),
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentManga.author?.ifBlank { null }
                                        ?: currentManga.artist?.ifBlank { null }.orEmpty(),
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.sp,
                                        color = DetailMuted,
                                        textAlign = TextAlign.Center,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
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
                                    text = "Chapters ($chapterCount)",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = DetailText,
                                    ),
                                )
                                if (readCount > 0L) {
                                    Text(
                                        text = "$readCount / $chapterCount read",
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

                    if (chaptersReady && activeChapters.isEmpty()) {
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
                                            context.appGraph.downloadManager.downloadChapters(
                                                currentManga,
                                                listOf(chapter),
                                            )
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
                onMigrateChapter = {
                    chapterToMigrate = targetChapter
                    selectedChapterForMenu = null
                },
            )
        }

        if (chapterToMigrate != null) {
            val migrateTarget = chapterToMigrate!!
            ChapterMigrationDialog(
                chapter = migrateTarget,
                manga = currentManga,
                onDismissRequest = { chapterToMigrate = null },
                searchChapterInOtherSources = searchChapterInOtherSources,
                onMigrate = { sourceId, sourceName, url, name, scanlator ->
                    onMigrateChapter?.invoke(
                        migrateTarget,
                        sourceId,
                        sourceName,
                        url,
                        name,
                        scanlator,
                    )
                    chapterToMigrate = null
                },
            )
        }

        if (showChangeCategoryDialogForCurrent) {
            val navigator = LocalNavigator.current
            val selection by produceState<List<CheckboxState<Category>>?>(null, currentManga.id) {
                val graph = context.appGraph
                val selected = graph.getCategories.await(currentManga.id).map { it.id }.toSet()
                value = graph.getCategories.await().filterNot { it.isSystemCategory }.map { category ->
                    if (category.id in
                        selected
                    ) {
                        CheckboxState.State.Checked(category)
                    } else {
                        CheckboxState.State.None(category)
                    }
                }
            }
            val initialSelection = selection
            if (initialSelection != null) {
                ChangeCategoryDialog(
                    initialSelection = initialSelection,
                    onDismissRequest = { showChangeCategoryDialogForCurrent = false },
                    onEditCategories = { navigator.push(CategoryRoute) },
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val customSourceName = chapter.memo["source_name"]?.let {
                    try {
                        it.toString().removeSurrounding("\"")
                    } catch (_: Exception) {
                        null
                    }
                }
                if (!customSourceName.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = customSourceName,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
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
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
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
    onMigrateChapter: () -> Unit = {},
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

                ChapterActionRow(
                    title = "Migrate to another extension",
                    icon = MaterialSymbols.Rounded.Sync,
                    onClick = onMigrateChapter,
                )
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
fun ChapterMigrationDialog(
    chapter: Chapter,
    manga: Manga,
    onDismissRequest: () -> Unit,
    searchChapterInOtherSources: (
        suspend (Manga, Chapter) -> List<MangaViewModel.ChapterMigrationCandidate>
    )?,
    onMigrate: (
        sourceId: Long,
        sourceName: String,
        url: String,
        name: String,
        scanlator: String?,
    ) -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var candidates by remember { mutableStateOf(emptyList<MangaViewModel.ChapterMigrationCandidate>()) }

    LaunchedEffect(chapter.id) {
        isLoading = true
        candidates = try {
            searchChapterInOtherSources?.invoke(manga, chapter) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = "Migrate Chapter",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DetailText,
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chapter.name,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = DetailMuted,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 360.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Searching other extensions...",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = DetailMuted,
                            ),
                        )
                    }
                } else if (candidates.isEmpty()) {
                    Text(
                        text = "No matching chapter found in other installed extensions.",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            color = DetailMuted,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(candidates) { candidate ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onMigrate(
                                            candidate.source.id,
                                            candidate.source.name,
                                            candidate.chapter.url,
                                            candidate.chapter.name,
                                            candidate.chapter.scanlator,
                                        )
                                    },
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                        ) {
                                            Text(
                                                text = candidate.source.name,
                                                style = TextStyle(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                        if (!candidate.chapter.scanlator.isNullOrBlank()) {
                                            Text(
                                                text = candidate.chapter.scanlator!!,
                                                style = TextStyle(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 11.sp,
                                                    color = DetailMuted,
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    Text(
                                        text = candidate.chapter.name,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = DetailText,
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "Close",
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
    val graph = LocalContext.current.appGraph
    val extension by produceState<String?>(null, manga.source) {
        value = graph.extensionManager.getLoadedExtensions()
            .firstOrNull { extension -> extension.sources.any { it.id == manga.source } }?.name
    }
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
                Text(
                    text = "Extension: ${extension ?: if (manga.source == 0L) "Local" else "Unavailable"}",
                    fontSize = 13.sp,
                    color = DetailMuted,
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                AnimatedContent(
                    targetState = categoryHeading,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "Title toolbar",
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DetailText,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.FilterList,
                        contentDescription = "Title options",
                        tint = DetailText,
                        modifier = Modifier.size(22.dp),
                    )
                }
                menu()
            }
        }
    }
}
