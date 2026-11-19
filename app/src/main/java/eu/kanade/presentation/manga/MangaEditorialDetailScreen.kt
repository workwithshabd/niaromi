package eu.kanade.presentation.manga

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.MoreVert
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import kotlin.math.absoluteValue

private val WarmIvoryBg = Color(0xFFFFFDF4)
private val NearBlackText = Color(0xFF171717)
private val MutedGrayText = Color(0xFF929292)
private val ButtonBlack = Color(0xFF1D1D1D)

@OptIn(ExperimentalMaterial3Api::class)
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
    onSelectManga: (Long) -> Unit,
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

    LaunchedEffect(pagerState, carouselMangaList) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page in carouselMangaList.indices) {
                val currentManga = carouselMangaList[page]
                if (currentManga.id != state.manga.id) {
                    onSelectManga(currentManga.id)
                }
            }
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showChaptersSheet by remember { mutableStateOf(false) }

    val currentManga = carouselMangaList.getOrNull(pagerState.currentPage) ?: state.manga
    val categoryHeading = currentManga.genre?.firstOrNull() ?: "MANGA DETAILS"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmIvoryBg),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DetailTopBar(
                categoryHeading = categoryHeading,
                onBack = navigateUp,
                onMenuClick = { showMenu = true },
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 72.dp),
                pageSpacing = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp),
            ) { page ->
                val mangaItem = carouselMangaList.getOrNull(page) ?: return@HorizontalPager
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val fraction = (1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
                val animatedScale = 0.86f + (fraction * 0.14f)
                val animatedAlpha = 0.45f + (fraction * 0.55f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(animatedScale)
                        .alpha(animatedAlpha),
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
                            .background(Color(0xFFEDE9DF)),
                    ) {
                        MangaCover.Book(
                            data = mangaItem.asMangaCover(),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                )

                Spacer(modifier = Modifier.height(4.dp))

                val author = currentManga.author?.ifBlank { null } ?: currentManga.artist ?: "Unknown Author"
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

                val chapterCount = state.processedChapters.size
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
                        onClick = { showChaptersSheet = true },
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

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                onClick = onContinueReading,
                shape = CircleShape,
                color = ButtonBlack,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp)
                    .widthIn(min = 125.dp, max = 150.dp)
                    .height(46.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = if (state.hasReadChapters) "Continue Reading" else "Read Now",
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
                    showChaptersSheet = true
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

        if (showChaptersSheet) {
            ModalBottomSheet(
                onDismissRequest = { showChaptersSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                containerColor = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Chapters (${state.processedChapters.size})",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = NearBlackText,
                        ),
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                    ) {
                        items(
                            items = state.processedChapters,
                            key = { it.chapter.id },
                        ) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showChaptersSheet = false
                                        onChapterClicked(item.chapter)
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = item.chapter.name,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 14.sp,
                                        color = if (item.chapter.read) MutedGrayText else NearBlackText,
                                        fontWeight = if (item.chapter.read) FontWeight.Normal else FontWeight.Medium,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (item.chapter.read) {
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
                }
            }
        }
    }
}

@Composable
fun DetailTopBar(
    categoryHeading: String,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 8.dp),
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
                modifier = Modifier.weight(1f),
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
