package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowLeft
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.presentation.core.components.material.PullRefresh
import kotlin.time.Duration.Companion.seconds

private val OffWhiteBackground = Color(0xFFF7F6F4)
private val DeepBlackText = Color(0xFF111111)
private val MutedCountText = Color(0xFF888888)

data class ShelfColorScheme(
    val topColor: Color,
    val bottomColor: Color,
    val borderColor: Color,
    val highlightColor: Color = Color.White.copy(alpha = 0.45f),
)

val SHELF_COLOR_SCHEMES = listOf(
    ShelfColorScheme(
        topColor = Color(0xFFF7B054).copy(alpha = 0.82f),
        bottomColor = Color(0xFFE88E20).copy(alpha = 0.88f),
        borderColor = Color(0xFFD67B14).copy(alpha = 0.70f),
    ),
    ShelfColorScheme(
        topColor = Color(0xFF63C4F5).copy(alpha = 0.82f),
        bottomColor = Color(0xFF35A3DC).copy(alpha = 0.88f),
        borderColor = Color(0xFF268DC2).copy(alpha = 0.70f),
    ),
    ShelfColorScheme(
        topColor = Color(0xFFF0BE56).copy(alpha = 0.82f),
        bottomColor = Color(0xFFD69424).copy(alpha = 0.88f),
        borderColor = Color(0xFFBF8017).copy(alpha = 0.70f),
    ),
    ShelfColorScheme(
        topColor = Color(0xFF67C2B9).copy(alpha = 0.82f),
        bottomColor = Color(0xFF3B9E94).copy(alpha = 0.88f),
        borderColor = Color(0xFF30867D).copy(alpha = 0.70f),
    ),
)

data class ShelfData(
    val id: Long,
    val name: String,
    val items: List<LibraryItem>,
    val category: Category?,
)

fun buildShelves(
    categories: List<Category>,
    favorites: List<LibraryItem>,
    getItemsForCategory: (Category) -> List<LibraryItem>,
    searchQuery: String?,
): List<ShelfData> {
    val query = searchQuery?.trim()
    val filterItem: (LibraryItem) -> Boolean = { item ->
        query.isNullOrEmpty() || item.libraryManga.manga.title.contains(query, ignoreCase = true)
    }

    val customCategories = categories.filterNot { it.isSystemCategory }
    if (customCategories.isNotEmpty()) {
        val list = mutableListOf<ShelfData>()
        customCategories.forEach { category ->
            val items = getItemsForCategory(category).filter(filterItem)
            if (items.isNotEmpty()) {
                list.add(
                    ShelfData(
                        id = category.id,
                        name = category.name,
                        items = items,
                        category = category,
                    ),
                )
            }
        }
        val defaultCategory = categories.find { it.isSystemCategory }
        if (defaultCategory != null) {
            val defaultItems = getItemsForCategory(defaultCategory).filter(filterItem)
            if (defaultItems.isNotEmpty()) {
                list.add(
                    ShelfData(
                        id = defaultCategory.id,
                        name = "Other",
                        items = defaultItems,
                        category = defaultCategory,
                    ),
                )
            }
        }
        if (list.isNotEmpty()) return list
    }

    val allFiltered = favorites.filter(filterItem)
    if (allFiltered.isEmpty()) return emptyList()

    val reading = allFiltered.filter { it.libraryManga.hasStarted && it.libraryManga.unreadCount > 0 }
        .sortedByDescending { it.libraryManga.lastRead }
    val planToRead = allFiltered.filter { !it.libraryManga.hasStarted }
        .sortedBy { it.libraryManga.manga.title.lowercase() }
    val completed = allFiltered.filter { it.libraryManga.hasStarted && it.libraryManga.unreadCount <= 0 }
        .sortedBy { it.libraryManga.manga.title.lowercase() }

    val defaultCategory = categories.firstOrNull()

    val shelves = mutableListOf<ShelfData>()
    if (reading.isNotEmpty()) {
        shelves.add(ShelfData(id = 1L, name = "Currently Reading", items = reading, category = defaultCategory))
    }
    if (planToRead.isNotEmpty()) {
        shelves.add(ShelfData(id = 2L, name = "Plan to Read", items = planToRead, category = defaultCategory))
    }
    if (completed.isNotEmpty()) {
        shelves.add(ShelfData(id = 3L, name = "Completed", items = completed, category = defaultCategory))
    }
    if (shelves.isEmpty()) {
        shelves.add(ShelfData(id = 0L, name = "Favourites", items = allFiltered, category = defaultCategory))
    }
    return shelves
}

@Composable
fun MangaBookshelfLibrary(
    categories: List<Category>,
    favorites: List<LibraryItem>,
    searchQuery: String?,
    selection: Set<Long>,
    contentPadding: PaddingValues,
    getItemsForCategory: (Category) -> List<LibraryItem>,
    onClickManga: (Long) -> Unit,
    onToggleSelection: (Category, LibraryManga) -> Unit,
    onRefresh: suspend () -> Boolean,
    onAddManga: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shelves = remember(categories, favorites, searchQuery) {
        buildShelves(categories, favorites, getItemsForCategory, searchQuery)
    }

    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OffWhiteBackground),
    ) {
        PullRefresh(
            refreshing = isRefreshing,
            enabled = selection.isEmpty(),
            onRefresh = {
                scope.launch {
                    if (!onRefresh()) return@launch
                    isRefreshing = true
                    delay(1.seconds)
                    isRefreshing = false
                }
            },
        ) {
            val hasTopBar = contentPadding.calculateTopPadding() > 0.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = 100.dp,
                ),
            ) {
                item(key = "editorial_header") {
                    EditorialLibraryHeader(
                        modifier = if (!hasTopBar) Modifier.statusBarsPadding() else Modifier,
                    )
                }

                items(
                    items = shelves,
                    key = { it.id },
                ) { shelf ->
                    val colorScheme = SHELF_COLOR_SCHEMES[shelves.indexOf(shelf) % SHELF_COLOR_SCHEMES.size]
                    MangaShelfSection(
                        shelf = shelf,
                        colorScheme = colorScheme,
                        selection = selection,
                        onClickManga = onClickManga,
                        onToggleSelection = { manga ->
                            shelf.category?.let { onToggleSelection(it, manga) }
                        },
                        modifier = Modifier.padding(bottom = 28.dp),
                    )
                }
            }
        }

        if (selection.isEmpty()) {
            FloatingAddMangaButton(
                onClick = onAddManga,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
fun EditorialLibraryHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "My Collection",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
                color = DeepBlackText,
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "MANGA",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 42.sp,
                letterSpacing = 2.sp,
                color = DeepBlackText,
            ),
        )
    }
}

@Composable
fun MangaShelfSection(
    shelf: ShelfData,
    colorScheme: ShelfColorScheme,
    selection: Set<Long>,
    onClickManga: (Long) -> Unit,
    onToggleSelection: (LibraryManga) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val canScrollBackward by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val canScrollForward by remember {
        derivedStateOf { listState.canScrollForward }
    }

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
                text = shelf.name,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = DeepBlackText,
                ),
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "${shelf.items.size} manga",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA),
                    ),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canScrollBackward) {
                            coroutineScope.launch {
                                val target = (listState.firstVisibleItemIndex - 3).coerceAtLeast(0)
                                listState.animateScrollToItem(target)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        tint = if (canScrollBackward) Color(0xFF555555) else Color(0xFFD0D0D0),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canScrollForward) {
                            coroutineScope.launch {
                                val target = (listState.firstVisibleItemIndex + 3).coerceAtMost(shelf.items.lastIndex)
                                listState.animateScrollToItem(target)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = if (canScrollForward) Color(0xFF555555) else Color(0xFFD0D0D0),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(134.dp),
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart),
            ) {
                items(
                    items = shelf.items,
                    key = { it.id },
                ) { item ->
                    MangaShelfCoverItem(
                        item = item,
                        isSelected = selection.contains(item.libraryManga.manga.id),
                        onClick = { onClickManga(item.libraryManga.manga.id) },
                        onLongClick = { onToggleSelection(item.libraryManga) },
                    )
                }
            }

            AcrylicShelfBar(
                colorScheme = colorScheme,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
fun MangaShelfCoverItem(
    item: LibraryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(86.dp)
            .height(126.dp)
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(3.dp),
                clip = false,
                ambientColor = Color(0x35000000),
                spotColor = Color(0x45000000),
            )
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFFE8E7E4))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        MangaCover.Book(
            data = item.libraryManga.manga.asMangaCover(),
            modifier = Modifier.fillMaxSize(),
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .border(2.dp, Color.White, RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
fun AcrylicShelfBar(
    colorScheme: ShelfColorScheme,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(7.dp),
                ambientColor = Color(0x25000000),
                spotColor = Color(0x35000000),
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(colorScheme.topColor, colorScheme.bottomColor),
                ),
                shape = RoundedCornerShape(7.dp),
            )
            .border(
                width = 1.dp,
                color = colorScheme.borderColor,
                shape = RoundedCornerShape(7.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .padding(horizontal = 6.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            colorScheme.highlightColor,
                            colorScheme.highlightColor,
                            Color.Transparent,
                        ),
                    ),
                    shape = RoundedCornerShape(1.dp),
                )
                .align(Alignment.TopCenter),
        )

        ShelfScrew(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp),
        )

        ShelfScrew(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
        )
    }
}

@Composable
fun ShelfScrew(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(9.dp)
            .shadow(1.dp, CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFEDEDED),
                        Color(0xFFC4C4C4),
                        Color(0xFFA0A0A0),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(9f, 9f),
                ),
                shape = CircleShape,
            )
            .border(0.5.dp, Color(0x33000000), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(1.dp)
                .background(Color(0xFF555555), RoundedCornerShape(0.5.dp)),
        )
    }
}

@Composable
fun FloatingAddMangaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = DeepBlackText,
        shadowElevation = 5.dp,
        modifier = modifier
            .widthIn(min = 130.dp, max = 150.dp)
            .height(44.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp),
        ) {
            Text(
                text = "Add Manga",
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
