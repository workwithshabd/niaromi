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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.ResultEffect
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import niaromi.BuildConfig
import niaromi.app.di.appGraph
import niaromi.core.navigation.AboutRoute
import niaromi.core.navigation.CategoryRoute
import niaromi.core.navigation.DownloadQueueRoute
import niaromi.core.navigation.ExtensionMigrationRoute
import niaromi.core.navigation.HistoryRoute
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.Navigator
import niaromi.core.navigation.NewUpdateRoute
import niaromi.core.navigation.SettingsSearchRoute
import niaromi.core.navigation.StatsRoute
import niaromi.data.updater.GITHUB_REPO
import niaromi.domain.release.interactor.GetApplicationRelease
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.automirroredrounded.ArrowBack
import niaromi.icons.materialsymbols.automirroredrounded.ChromeReaderMode
import niaromi.icons.materialsymbols.automirroredrounded.Label
import niaromi.icons.materialsymbols.rounded.CloudOff
import niaromi.icons.materialsymbols.rounded.Code
import niaromi.icons.materialsymbols.rounded.CollectionsBookmark
import niaromi.icons.materialsymbols.rounded.Download
import niaromi.icons.materialsymbols.rounded.Explore
import niaromi.icons.materialsymbols.rounded.Eyeglasses2
import niaromi.icons.materialsymbols.rounded.Info
import niaromi.icons.materialsymbols.rounded.KeyboardArrowRight
import niaromi.icons.materialsymbols.rounded.Palette
import niaromi.icons.materialsymbols.rounded.QueryStats
import niaromi.icons.materialsymbols.rounded.Schedule
import niaromi.icons.materialsymbols.rounded.Search
import niaromi.icons.materialsymbols.rounded.Security
import niaromi.icons.materialsymbols.rounded.Storage
import niaromi.icons.materialsymbols.rounded.Sync
import niaromi.icons.simpleicons.Github
import niaromi.icons.simpleicons.SimpleIcons
import niaromi.presentation.core.components.material.Scaffold
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.core.util.collectAsState
import niaromi.presentation.core.util.isTabletUi
import niaromi.presentation.more.settings.screen.about.getVersionName
import niaromi.ui.home.TopLevelRoute
import tachiyomi.i18n.MR

private val SettingsBg: Color
    @Composable get() = MaterialTheme.colorScheme.background

private val SettingsText: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

private val SettingsMutedText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val SettingsCardBg: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainer

private val SettingsCardBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant

private val SettingsSearchBarBg: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

private val SettingsSearchBarBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant

private val SettingsIconBg: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest

private val SettingsDividerColor: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

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
        val containerColor = if (twoPane) getPalerSurface() else SettingsBg
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

        var showGitHubUpdateDialog by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val state = rememberLazyListState()

        ResultEffect<Unit>(resultKey = TopLevelRoute.More.reselectEventKey) {
            scope.launch { state.animateScrollToItem(0) }
        }

        val downloadedOnlyPref = remember { context.appGraph.basePreferences.downloadedOnly }
        val downloadedOnly by downloadedOnlyPref.collectAsState()

        val incognitoModePref = remember { context.appGraph.basePreferences.incognitoMode }
        val incognitoMode by incognitoModePref.collectAsState()

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
                                        tint = SettingsText,
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
                                        color = SettingsMutedText,
                                    ),
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Settings",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        color = SettingsText,
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
                                .background(SettingsSearchBarBg, RoundedCornerShape(14.dp))
                                .border(0.5.dp, SettingsSearchBarBorder, RoundedCornerShape(14.dp))
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
                                    tint = SettingsMutedText,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Search settings...",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 14.sp,
                                        color = SettingsMutedText,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    item(key = "quick_controls_card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 16.dp, bottom = 4.dp),
                        ) {
                            Text(
                                text = "QUICK CONTROLS & TOOLS",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.2.sp,
                                    color = SettingsMutedText,
                                ),
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(SettingsCardBg)
                                    .border(0.5.dp, SettingsCardBorder, RoundedCornerShape(18.dp)),
                            ) {
                                QuickToggleRow(
                                    title = "Downloaded only",
                                    subtitle = "Only show titles that are saved locally on device",
                                    icon = MaterialSymbols.Rounded.CloudOff,
                                    checked = downloadedOnly,
                                    onCheckedChange = { downloadedOnlyPref.set(it) },
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = SettingsDividerColor,
                                )

                                QuickToggleRow(
                                    title = "Incognito mode",
                                    subtitle = "Pauses reading history and download tracking",
                                    icon = MaterialSymbols.Rounded.Eyeglasses2,
                                    checked = incognitoMode,
                                    onCheckedChange = { incognitoModePref.set(it) },
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = SettingsDividerColor,
                                )

                                QuickToolRow(
                                    title = "Download queue",
                                    subtitle = "View and manage active and pending downloads",
                                    icon = MaterialSymbols.Rounded.Download,
                                    onClick = { navigator.push(DownloadQueueRoute) },
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = SettingsDividerColor,
                                )

                                QuickToolRow(
                                    title = "Categories",
                                    subtitle = "Create, sort and manage custom library categories",
                                    icon = MaterialSymbols.AutoMirroredRounded.Label,
                                    onClick = { navigator.push(CategoryRoute) },
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = SettingsDividerColor,
                                )

                                QuickToolRow(
                                    title = "Reading history",
                                    subtitle = "View recently read chapters and resume reading",
                                    icon = MaterialSymbols.Rounded.Schedule,
                                    onClick = { navigator.push(HistoryRoute) },
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = SettingsDividerColor,
                                )

                                QuickToolRow(
                                    title = "Statistics",
                                    subtitle = "View total chapters read, time spent and charts",
                                    icon = MaterialSymbols.Rounded.QueryStats,
                                    onClick = { navigator.push(StatsRoute) },
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
                                        color = SettingsMutedText,
                                    ),
                                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(SettingsCardBg)
                                        .border(0.5.dp, SettingsCardBorder, RoundedCornerShape(18.dp)),
                                ) {
                                    section.items.forEachIndexed { itemIndex, item ->
                                        val isSelected = twoPane && (selectedScreen == item.screen)
                                        SettingsItemRow(
                                            item = item,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (item.isUpdateAction) {
                                                    showGitHubUpdateDialog = true
                                                } else if (item.screen != null) {
                                                    navigator.navigate(item.screen, twoPane)
                                                }
                                            },
                                        )
                                        if (itemIndex < section.items.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                                                thickness = 0.5.dp,
                                                color = SettingsDividerColor,
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

        if (showGitHubUpdateDialog) {
            GitHubUpdateDialog(
                onDismissRequest = { showGitHubUpdateDialog = false },
            )
        }
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
        val screen: NavKey? = null,
        val isUpdateAction: Boolean = false,
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
            titleRes = MR.strings.history,
            formatSubtitle = { "Recently read chapters and progress" },
            icon = MaterialSymbols.Rounded.Schedule,
            screen = HistoryRoute,
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
            titleRes = MR.strings.check_for_updates,
            formatSubtitle = {
                "Check and push updates from GitHub ($GITHUB_REPO)"
            },
            icon = SimpleIcons.Github,
            isUpdateAction = true,
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
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
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
                .background(SettingsIconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = SettingsText,
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
                    color = SettingsText,
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
                        color = SettingsMutedText,
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
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun QuickToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange(!checked) })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(SettingsIconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SettingsText,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = SettingsText,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.5.sp,
                    color = SettingsMutedText,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
    }
}

@Composable
private fun QuickToolRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(SettingsIconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SettingsText,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = SettingsText,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.5.sp,
                    color = SettingsMutedText,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = MaterialSymbols.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
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

@Composable
fun GitHubUpdateDialog(
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val updateChecker = remember { context.appGraph.updateChecker }

    var statusText by remember { mutableStateOf("Press 'Check for Updates' to query latest releases from GitHub.") }
    var isChecking by remember { mutableStateOf(false) }
    var newRelease by remember { mutableStateOf<niaromi.domain.release.model.Release?>(null) }

    val onCheckClick: () -> Unit = {
        if (!isChecking) {
            scope.launch {
                isChecking = true
                statusText = "Checking GitHub ($GITHUB_REPO)..."
                newRelease = null
                try {
                    when (val result = updateChecker.checkForUpdate(forceCheck = true)) {
                        is GetApplicationRelease.Result.NewUpdate -> {
                            newRelease = result.release
                            statusText = "New release found: ${result.release.version}"
                        }
                        is GetApplicationRelease.Result.NoNewUpdate -> {
                            statusText = "Up to date. No new releases found on GitHub ($GITHUB_REPO)."
                        }
                        is GetApplicationRelease.Result.OsTooOld -> {
                            statusText = "Device OS version is not supported for updates."
                        }
                    }
                } catch (e: Exception) {
                    statusText = "Unable to connect to GitHub releases: ${e.message ?: "Unknown error"}"
                } finally {
                    isChecking = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            if (newRelease != null) {
                TextButton(
                    onClick = {
                        val release = newRelease!!
                        onDismissRequest()
                        navigator.push(
                            NewUpdateRoute(
                                versionName = release.version,
                                changelogInfo = release.info,
                                releaseLink = release.releaseLink,
                                downloadLink = release.downloadLink,
                            ),
                        )
                    },
                ) {
                    Text("Install Update")
                }
            } else {
                TextButton(
                    onClick = onCheckClick,
                    enabled = !isChecking,
                ) {
                    Text(if (isChecking) "Checking..." else "Check for Updates")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        },
        title = {
            Text("GitHub Updates")
        },
        text = {
            Column {
                Text(
                    text = "Current: Niaromi ${BuildConfig.VERSION_NAME} (r${BuildConfig.COMMIT_COUNT})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Repository: $GITHUB_REPO",
                    style = MaterialTheme.typography.bodySmall,
                    color = SettingsMutedText,
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (isChecking) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
    )
}
