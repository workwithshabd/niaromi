package niaromi.presentation.more.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import niaromi.presentation.library.components.SearchHeaderHeight
import niaromi.presentation.library.components.searchBackdrop
import niaromi.presentation.more.settings.screen.about.getVersionName
import niaromi.ui.home.TopLevelRoute
import tachiyomi.i18n.MR

private val SettingsBg: Color
    @Composable get() = MaterialTheme.colorScheme.background

private val SettingsMutedText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val SettingsCardBg: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainer

private val SettingsSearchBarBg: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

private val SettingsDividerColor: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

@Serializable
data object SettingsMainScreen : NavKey {

    @Composable
    fun Content(navigateUp: (() -> Unit)? = null) {
        val navigator = LocalNavigator.current
        val twoPane = isTabletUi()
        val containerColor = SettingsBg
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

        val scrollBehavior = if (canNavigateUp || twoPane) {
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)
        } else {
            TopAppBarDefaults.pinnedScrollBehavior(topBarState)
        }

        val selectedScreen = if (twoPane) {
            navigator.routes.takeLastWhile { it != SettingsMainScreen }.firstOrNull()
        } else {
            null
        }

        var showGitHubUpdateDialog by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val state = rememberLazyListState()

        ResultEffect<Unit>(resultKey = TopLevelRoute.More.reselectEventKey) {
            state.requestScrollToItem(0)
        }

        val downloadedOnlyPref = remember { context.appGraph.basePreferences.downloadedOnly }
        val downloadedOnly by downloadedOnlyPref.collectAsState()

        val incognitoModePref = remember { context.appGraph.basePreferences.incognitoMode }
        val incognitoMode by incognitoModePref.collectAsState()

        LaunchedEffect(selectedScreen) {
            if (selectedScreen != null) {
                var firstRow = 9
                for (section in sections) {
                    val index = section.items.indexOfFirst { it.screen == selectedScreen }
                    if (index >= 0) {
                        state.requestScrollToItem(firstRow + index)
                        break
                    }
                    firstRow += section.items.size + 1
                }
            }
        }

        Scaffold(
            containerColor = containerColor,
            topBarScrollBehavior = scrollBehavior,
            topBar = {
                if (canNavigateUp || twoPane) {
                    LargeTopAppBar(
                        title = { Text("Settings") },
                        navigationIcon = {
                            if (canNavigateUp) {
                                IconButton(onClick = { onBack?.invoke() }) {
                                    Icon(
                                        imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                                        contentDescription = stringResource(MR.strings.action_bar_up_description),
                                    )
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior,
                    )
                }
            },
            content = { contentPadding ->
                Box(
                    Modifier.fillMaxSize().background(
                        containerColor,
                    ).padding(top = contentPadding.calculateTopPadding()),
                ) {
                    val scrolled by remember(state) {
                        derivedStateOf { state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0 }
                    }
                    LazyColumn(
                        state = state,
                        modifier = Modifier.fillMaxSize().searchBackdrop(scrolled),
                        contentPadding = PaddingValues(top = SearchHeaderHeight, bottom = 120.dp),
                    ) {
                        item(key = "quick_controls_header") {
                            SettingsGroupHeading("Quick controls & tools")
                        }

                        item(key = "quick_downloaded_only") {
                            SettingsGroupRow(first = true, last = false) {
                                QuickToggleRow(
                                    title = "Downloaded only",
                                    subtitle = "Only show titles that are saved locally on device",
                                    icon = MaterialSymbols.Rounded.CloudOff,
                                    checked = downloadedOnly,
                                    onCheckedChange = { downloadedOnlyPref.set(it) },
                                )
                            }
                        }

                        item(key = "quick_incognito") {
                            SettingsGroupRow(first = false, last = false) {
                                QuickToggleRow(
                                    title = "Incognito mode",
                                    subtitle = "Pauses reading history and download tracking",
                                    icon = MaterialSymbols.Rounded.Eyeglasses2,
                                    checked = incognitoMode,
                                    onCheckedChange = { incognitoModePref.set(it) },
                                )
                            }
                        }

                        item(key = "quick_download_queue") {
                            SettingsGroupRow(first = false, last = false) {
                                QuickToolRow(
                                    title = "Download queue",
                                    subtitle = "View and manage active and pending downloads",
                                    icon = MaterialSymbols.Rounded.Download,
                                    onClick = { navigator.push(DownloadQueueRoute) },
                                )
                            }
                        }

                        item(key = "quick_categories") {
                            SettingsGroupRow(first = false, last = false) {
                                QuickToolRow(
                                    title = "Categories",
                                    subtitle = "Create, sort and manage custom library categories",
                                    icon = MaterialSymbols.AutoMirroredRounded.Label,
                                    onClick = { navigator.push(CategoryRoute) },
                                )
                            }
                        }

                        item(key = "quick_history") {
                            SettingsGroupRow(first = false, last = false) {
                                QuickToolRow(
                                    title = "Reading history",
                                    subtitle = "View recently read chapters and resume reading",
                                    icon = MaterialSymbols.Rounded.Schedule,
                                    onClick = { navigator.push(HistoryRoute) },
                                )
                            }
                        }

                        item(key = "quick_statistics") {
                            SettingsGroupRow(first = false, last = true) {
                                QuickToolRow(
                                    title = "Statistics",
                                    subtitle = "View total chapters read, time spent and charts",
                                    icon = MaterialSymbols.Rounded.QueryStats,
                                    onClick = { navigator.push(StatsRoute) },
                                )
                            }
                        }

                        sections.forEach { section ->
                            item(key = "section_${section.title}") {
                                SettingsGroupHeading(section.title)
                            }
                            itemsIndexed(section.items, key = { _, item ->
                                "${section.title}:${item.titleRes}"
                            }) { index, item ->
                                SettingsGroupRow(first = index == 0, last = index == section.items.lastIndex) {
                                    SettingsItemRow(
                                        item = item,
                                        isSelected = twoPane && selectedScreen == item.screen,
                                        onClick = {
                                            if (item.isUpdateAction) {
                                                showGitHubUpdateDialog = true
                                            } else if (item.screen != null) {
                                                navigator.navigate(item.screen, twoPane)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(SettingsSearchBarBg)
                            .clickable { navigator.navigate(SettingsSearchRoute, twoPane) }
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
                                style = MaterialTheme.typography.bodyLarge,
                                color = SettingsMutedText,
                                modifier = Modifier.weight(1f),
                            )
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
            screen = ExtensionMigrationRoute(),
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
            title = "Reading & content",
            items = readingItems,
        ),
        SettingsSection(
            title = "Appearance & security",
            items = appearanceSecurityItems,
        ),
        SettingsSection(
            title = "Data & system",
            items = storageSystemItems,
        ),
    )
}

@Composable
private fun SettingsGroupHeading(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsGroupRow(
    first: Boolean,
    last: Boolean,
    content: @Composable () -> Unit,
) {
    val top = if (first) 24.dp else 0.dp
    val bottom = if (last) 24.dp else 0.dp
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(top, top, bottom, bottom))
            .background(SettingsCardBg),
    ) {
        content()
        if (!last) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = SettingsDividerColor,
            )
        }
    }
}

@Composable
private fun SettingsItemRow(
    item: SettingsMainScreen.Item,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    SettingsListItem(
        title = stringResource(item.titleRes),
        subtitle = item.formatSubtitle(),
        icon = item.icon,
        modifier = Modifier.clickable(onClick = onClick),
        isSelected = isSelected,
    )
}

@Composable
private fun QuickToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsListItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@Composable
private fun QuickToolRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    SettingsListItem(title, subtitle, icon, Modifier.clickable(onClick = onClick))
}

@Composable
private fun SettingsListItem(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    modifier: Modifier,
    isSelected: Boolean = false,
    trailingContent: @Composable () -> Unit = {
        Icon(MaterialSymbols.Rounded.KeyboardArrowRight, contentDescription = null)
    },
) {
    val colors = MaterialTheme.colorScheme
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.takeIf { it.isNotBlank() }?.let { summary -> { Text(summary) } },
        leadingContent = {
            Box(
                modifier = Modifier.size(40.dp).background(colors.secondaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.onSecondaryContainer)
            }
        },
        trailingContent = trailingContent,
        modifier = modifier,
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) colors.secondaryContainer else colors.surfaceContainer,
        ),
    )
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
