package niaromi.presentation.more.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.navigation3.runtime.NavKey
import dev.icerock.moko.resources.StringResource
import kotlinx.serialization.Serializable
import niaromi.core.navigation.AboutRoute
import niaromi.core.navigation.ExtensionMigrationRoute
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.Navigator
import niaromi.core.navigation.SettingsSearchRoute
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.automirroredrounded.ArrowBack
import niaromi.icons.materialsymbols.automirroredrounded.ChromeReaderMode
import niaromi.icons.materialsymbols.rounded.Code
import niaromi.icons.materialsymbols.rounded.CollectionsBookmark
import niaromi.icons.materialsymbols.rounded.Download
import niaromi.icons.materialsymbols.rounded.Explore
import niaromi.icons.materialsymbols.rounded.Info
import niaromi.icons.materialsymbols.rounded.KeyboardArrowRight
import niaromi.icons.materialsymbols.rounded.Palette
import niaromi.icons.materialsymbols.rounded.Search
import niaromi.icons.materialsymbols.rounded.Security
import niaromi.icons.materialsymbols.rounded.Storage
import niaromi.icons.materialsymbols.rounded.Sync
import niaromi.presentation.core.components.material.Scaffold
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.core.util.isTabletUi
import niaromi.presentation.more.settings.screen.about.getVersionName
import tachiyomi.i18n.MR

private val WarmIvoryBg = Color(0xFFFFFDF4)
private val NearBlackText = Color(0xFF171717)
private val MutedGrayText = Color(0xFF929292)
private val CardBg = Color(0xFFFBF9F1)
private val CardBorder = Color(0xFFE9E5D8)
private val SearchBarBg = Color(0xFFF4F1E6)
private val SearchBarBorder = Color(0xFFE9E5D8)
private val IconBg = Color(0xFFECE7DA)
private val DividerColor = Color(0xFFEFECE2)

@Serializable
data object SettingsMainScreen : NavKey {

    @Composable
    private fun getPalerSurface(): Color {
        val surface = MaterialTheme.colorScheme.surface
        val dark = isSystemInDarkTheme()
        return remember(surface, dark) {
            val arr = FloatArray(3)
            ColorUtils.colorToHSL(surface.toArgb(), arr)
            arr[2] = if (dark) {
                arr[2] - 0.05f
            } else {
                arr[2] + 0.02f
            }.coerceIn(0f, 1f)
            Color.hsl(arr[0], arr[1], arr[2])
        }
    }

    @Composable
    fun Content(navigateUp: (() -> Unit)? = null) {
        val navigator = LocalNavigator.current
        val twoPane = isTabletUi()
        val containerColor = if (twoPane) getPalerSurface() else WarmIvoryBg
        val topBarState = rememberTopAppBarState()

        val canNavigateUp = navigateUp != null || (!twoPane && navigator.routes.size > 1)
        val onBack = navigateUp ?: if (!twoPane && navigator.routes.size > 1) {
            {
                navigator.popTo { it == SettingsMainScreen }
                navigator.pop()
            }
        } else {
            null
        }

        val selectedScreen = if (twoPane) {
            navigator.routes.takeLastWhile { it != SettingsMainScreen }.firstOrNull()
        } else {
            null
        }

        val state = rememberLazyListState()

        if (twoPane && selectedScreen != null) {
            val index = items.indexOfFirst { it.screen == selectedScreen }
            if (index > -1) {
                LaunchedEffect(index) {
                    state.animateScrollToItem(index)
                    if (index > 0) {
                        topBarState.contentOffset = topBarState.heightOffsetLimit
                    }
                }
            }
        }

        Scaffold(
            containerColor = containerColor,
            content = { contentPadding ->
                LazyColumn(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(containerColor),
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        bottom = 120.dp,
                    ),
                ) {
                    item(key = "editorial_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (canNavigateUp) {
                                IconButton(
                                    onClick = { onBack?.invoke() },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                                        contentDescription = stringResource(MR.strings.action_bar_up_description),
                                        tint = NearBlackText,
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(40.dp))
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "PREFERENCES",
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
                                    text = "Settings",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        color = NearBlackText,
                                        textAlign = TextAlign.Center,
                                    ),
                                )
                            }

                            Spacer(modifier = Modifier.size(40.dp))
                        }
                    }

                    item(key = "search_bar") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .height(44.dp)
                                .background(SearchBarBg, RoundedCornerShape(14.dp))
                                .border(0.5.dp, SearchBarBorder, RoundedCornerShape(14.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { navigator.navigate(SettingsSearchRoute, twoPane) },
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.Search,
                                    contentDescription = stringResource(MR.strings.action_search),
                                    tint = Color(0xFF888888),
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Search settings...",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 14.sp,
                                        color = Color(0xFF999999),
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    sections.forEach { section ->
                        item(key = "section_${section.title}") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 22.dp, bottom = 4.dp),
                            ) {
                                Text(
                                    text = section.title,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        letterSpacing = 1.2.sp,
                                        color = MutedGrayText,
                                    ),
                                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(CardBg)
                                        .border(0.5.dp, CardBorder, RoundedCornerShape(18.dp)),
                                ) {
                                    section.items.forEachIndexed { itemIndex, item ->
                                        val isSelected = twoPane && (selectedScreen == item.screen)
                                        SettingsItemRow(
                                            item = item,
                                            isSelected = isSelected,
                                            onClick = { navigator.navigate(item.screen, twoPane) },
                                        )
                                        if (itemIndex < section.items.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                                                thickness = 0.5.dp,
                                                color = DividerColor,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
        )
    }

    private fun Navigator.navigate(screen: NavKey, twoPane: Boolean) {
        if (twoPane) popTo { it == SettingsMainScreen }
        push(screen)
    }

    internal data class Item(
        val titleRes: StringResource,
        val subtitleRes: StringResource? = null,
        val formatSubtitle: @Composable () -> String? = { subtitleRes?.let { stringResource(it) } },
        val icon: ImageVector,
        val screen: NavKey,
    )

    private data class SettingsSection(
        val title: String,
        val items: List<Item>,
    )

    private val readingItems = listOf(
        Item(
            titleRes = MR.strings.pref_category_library,
            subtitleRes = MR.strings.pref_library_summary,
            icon = MaterialSymbols.Rounded.CollectionsBookmark,
            screen = SettingsLibraryScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_reader,
            subtitleRes = MR.strings.pref_reader_summary,
            icon = MaterialSymbols.AutoMirroredRounded.ChromeReaderMode,
            screen = SettingsReaderScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_downloads,
            subtitleRes = MR.strings.pref_downloads_summary,
            icon = MaterialSymbols.Rounded.Download,
            screen = SettingsDownloadScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_tracking,
            subtitleRes = MR.strings.pref_tracking_summary,
            icon = MaterialSymbols.Rounded.Sync,
            screen = SettingsTrackingScreen,
        ),
        Item(
            titleRes = MR.strings.label_extensions,
            formatSubtitle = { "Manage installed extensions and migrate sources" },
            icon = MaterialSymbols.Rounded.Explore,
            screen = ExtensionMigrationRoute,
        ),
        Item(
            titleRes = MR.strings.browse,
            subtitleRes = MR.strings.pref_browse_summary,
            icon = MaterialSymbols.Rounded.Explore,
            screen = SettingsBrowseScreen,
        ),
    )

    private val appearanceSecurityItems = listOf(
        Item(
            titleRes = MR.strings.pref_category_appearance,
            subtitleRes = MR.strings.pref_appearance_summary,
            icon = MaterialSymbols.Rounded.Palette,
            screen = SettingsAppearanceScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_security,
            subtitleRes = MR.strings.pref_security_summary,
            icon = MaterialSymbols.Rounded.Security,
            screen = SettingsSecurityScreen,
        ),
    )

    private val storageSystemItems = listOf(
        Item(
            titleRes = MR.strings.label_data_storage,
            subtitleRes = MR.strings.pref_backup_summary,
            icon = MaterialSymbols.Rounded.Storage,
            screen = SettingsDataScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_advanced,
            subtitleRes = MR.strings.pref_advanced_summary,
            icon = MaterialSymbols.Rounded.Code,
            screen = SettingsAdvancedScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_about,
            formatSubtitle = {
                "${stringResource(MR.strings.app_name)} ${getVersionName(buildTime = null)}"
            },
            icon = MaterialSymbols.Rounded.Info,
            screen = AboutRoute,
        ),
    )

    private val sections = listOf(
        SettingsSection(
            title = "READING & CONTENT",
            items = readingItems,
        ),
        SettingsSection(
            title = "APPEARANCE & SECURITY",
            items = appearanceSecurityItems,
        ),
        SettingsSection(
            title = "DATA & SYSTEM",
            items = storageSystemItems,
        ),
    )

    private val items = readingItems + appearanceSecurityItems + storageSystemItems
}

@Composable
private fun SettingsItemRow(
    item: SettingsMainScreen.Item,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.background(Color(0xFFEAE5D8))
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(IconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = NearBlackText,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(item.titleRes),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = NearBlackText,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = item.formatSubtitle()
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        color = MutedGrayText,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = MaterialSymbols.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFB0AAA0),
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Opens settings at [screen], or at the main screen if null. In two panes, reuses open settings as the list pane and
 * shows Appearance when no [screen] is given.
 */
fun Navigator.openSettings(screen: NavKey? = null, twoPane: Boolean) {
    if (!twoPane) {
        push(screen ?: SettingsMainScreen)
        return
    }
    if (!popTo { it == SettingsMainScreen }) push(SettingsMainScreen)
    push(screen ?: SettingsAppearanceScreen)
}
