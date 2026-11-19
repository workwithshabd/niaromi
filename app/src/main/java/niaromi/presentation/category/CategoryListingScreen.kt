package niaromi.presentation.category

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.navigation3.runtime.result.LocalResultEventBus
import niaromi.app.di.appGraph
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.MangaRoute
import niaromi.domain.manga.model.Manga
import niaromi.domain.manga.model.asMangaCover
import niaromi.icons.materialsymbols.automirroredrounded.ArrowBack
import niaromi.icons.materialsymbols.rounded.MoreVert
import niaromi.presentation.components.FloatingBottomNavigationBar
import niaromi.presentation.manga.components.MangaCover
import niaromi.ui.home.TopLevelRoute
import niaromi.ui.library.LibrarySearchEvent

private val ListingBg: Color
    @Composable get() = MaterialTheme.colorScheme.background

private val ListingText: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

private val ListingMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun CategoryListingScreen(
    categoryId: Long,
    categoryName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val resultEventBus = LocalResultEventBus.current
    val libraryMangaList by produceState(initialValue = emptyList<niaromi.domain.library.model.LibraryManga>()) {
        value = context.appGraph.getLibraryManga.await()
    }

    val filteredManga = remember(libraryMangaList, categoryId, categoryName) {
        when {
            categoryId in 1000L..9998L -> {
                libraryMangaList.filter { item ->
                    val primary = item.manga.genre?.firstOrNull { it.isNotBlank() }
                        ?.trim()?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    primary.equals(categoryName, ignoreCase = true) ||
                        item.manga.genre?.any { it.equals(categoryName, ignoreCase = true) } == true
                }.map { it.manga }
            }
            categoryId in 1L..999L -> {
                libraryMangaList.filter { item ->
                    categoryId in item.categories
                }.map { it.manga }
            }
            else -> libraryMangaList.map { it.manga }
        }.ifEmpty { libraryMangaList.map { it.manga } }
    }

    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ListingBg),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            CategoryListingTopBar(
                categoryName = categoryName,
                onBack = { navigator.pop() },
                onMenuClick = { showMenu = true },
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(
                    items = filteredManga,
                    key = { it.id },
                ) { item ->
                    CategoryMangaGridItem(
                        item = item,
                        onClick = { navigator.push(MangaRoute(item.id)) },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Sort by title") },
                onClick = { showMenu = false },
            )
            DropdownMenuItem(
                text = { Text("Sort by unread") },
                onClick = { showMenu = false },
            )
        }

        FloatingBottomNavigationBar(
            currentRoute = TopLevelRoute.Library,
            onSelectRoute = { route ->
                if (route == TopLevelRoute.Library) {
                    resultEventBus.sendResult(LibrarySearchEvent(""))
                }
                navigator.pop()
            },
            onReselectRoute = { route ->
                if (route == TopLevelRoute.Library) {
                    resultEventBus.sendResult(LibrarySearchEvent(""))
                }
                navigator.pop()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
fun CategoryListingTopBar(
    categoryName: String,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 12.dp),
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
                    tint = ListingText,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "ALL MANGA FROM",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        color = ListingMuted,
                    ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = categoryName,
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = ListingText,
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
                    tint = ListingText,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
fun CategoryMangaGridItem(
    item: Manga,
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
                .aspectRatio(0.68f)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(6.dp),
                    ambientColor = Color(0x20000000),
                    spotColor = Color(0x30000000),
                )
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            MangaCover.Book(
                data = item.asMangaCover(),
                modifier = Modifier.fillMaxSize(),
                onClick = onClick,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = ListingText,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(2.dp))

        val authorText = item.author?.ifBlank { null } ?: item.artist ?: "Unknown Author"
        Text(
            text = authorText,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = ListingMuted,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
