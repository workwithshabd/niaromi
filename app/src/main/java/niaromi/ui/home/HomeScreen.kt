package niaromi.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.Serializable
import niaromi.R
import niaromi.app.di.appGraph
import niaromi.core.navigation.DownloadQueueRoute
import niaromi.core.navigation.LatestChaptersRoute
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.MangaRoute
import niaromi.core.navigation.Navigator
import niaromi.core.navigation.NavigatorDisplay
import niaromi.core.navigation.rememberNavigator
import niaromi.presentation.components.MainNavigationBar
import niaromi.presentation.core.i18n.pluralStringResource
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.core.util.isTabletUi
import niaromi.presentation.util.rememberAnimatedVectorPainter
import niaromi.ui.browse.BrowseShowExtensionsEvent
import niaromi.ui.browse.BrowseTab
import niaromi.ui.explore.ExploreTab
import niaromi.ui.library.LibrarySearchEvent
import niaromi.ui.library.LibraryTab
import tachiyomi.i18n.MR

@Serializable
sealed interface TopLevelRoute : NavKey {

    val reselectEventKey: String
        get() = "TabReselectEvent:$this"

    @Composable
    fun options(isSelected: Boolean): TabOptions

    @Serializable
    data object Library : TopLevelRoute {
        @Composable
        override fun options(isSelected: Boolean): TabOptions {
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.animated_library)
            return TabOptions(
                title = stringResource(MR.strings.label_library),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }
    }

    @Serializable
    data object Explore : TopLevelRoute {
        @Composable
        override fun options(isSelected: Boolean): TabOptions {
            val enter = AnimatedImageVector.animatedVectorResource(R.drawable.animated_browse_enter)
            val exit = AnimatedImageVector.animatedVectorResource(R.drawable.animated_browse_exit)
            return TabOptions(
                title = "Explore",
                icon = rememberAnimatedVectorPainter(enter, exit, isSelected),
            )
        }
    }

    @Serializable
    data object Browse : TopLevelRoute {
        @Composable
        override fun options(isSelected: Boolean): TabOptions {
            val enter = AnimatedImageVector.animatedVectorResource(R.drawable.animated_browse_enter)
            val exit = AnimatedImageVector.animatedVectorResource(R.drawable.animated_browse_exit)
            return TabOptions(
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(enter, exit, isSelected),
            )
        }
    }

    @Serializable
    data object More : TopLevelRoute {
        @Composable
        override fun options(isSelected: Boolean): TabOptions {
            val enter = AnimatedImageVector.animatedVectorResource(R.drawable.animated_more_enter)
            val exit = AnimatedImageVector.animatedVectorResource(R.drawable.animated_more_exit)
            return TabOptions(
                title = stringResource(MR.strings.label_more),
                icon = rememberAnimatedVectorPainter(enter, exit, isSelected),
            )
        }
    }
}

@Composable
fun HomeScreen() {
    val tabNavigator = key("home-with-latest-chapters") { rememberNavigator(TopLevelRoute.Library) }
    val navigator = LocalNavigator.current
    val resultEventBus = LocalResultEventBus.current
    val tabletUi = isTabletUi()
    val navigationSuiteType = if (tabletUi) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationBar
    }
    val navigationSuiteState = rememberNavigationSuiteScaffoldState()
    var showBottomNav by remember { mutableStateOf(true) }
    LaunchedEffect(tabletUi) {
        if (tabletUi && showBottomNav) navigationSuiteState.show() else navigationSuiteState.hide()
    }
    ResultEffect<ShowBottomNavEvent> {
        showBottomNav = it.visible
        if (tabletUi) {
            if (it.visible) navigationSuiteState.show() else navigationSuiteState.hide()
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteType = navigationSuiteType,
        state = navigationSuiteState,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationRailContainerColor = MaterialTheme.colorScheme
                .surfaceColorAtElevation(3.dp),
        ),
        navigationItemVerticalArrangement = Arrangement.Center,
        navigationItems = {
            if (tabletUi) TABS.fastForEach { NavigationSuiteItem(it, tabNavigator, navigationSuiteType) }
        },
    ) {
        val tabStateHolder = rememberSaveableStateHolder()
        Box(modifier = Modifier.fillMaxSize()) {
            NavigatorDisplay(
                navigator = tabNavigator,
                modifier = Modifier.fillMaxSize(),
                entryDecorators = listOf(
                    remember(tabStateHolder) {
                        NavEntryDecorator { entry ->
                            tabStateHolder.SaveableStateProvider(entry.contentKey) { entry.Content() }
                        }
                    },
                ),
                transitionSpec = { spec },
                popTransitionSpec = { spec },
                predictivePopTransitionSpec = { spec },
                entryProvider = entryProvider {
                    entry<TopLevelRoute.Library> {
                        LibraryTab()
                    }

                    entry<TopLevelRoute.Explore> {
                        ExploreTab()
                    }

                    entry<TopLevelRoute.Browse> {
                        BrowseTab()
                    }

                    entry<TopLevelRoute.More> {
                        niaromi.presentation.more.settings.screen.SettingsMainScreen.Content()
                    }
                },
            )

            if (!tabletUi && showBottomNav) {
                MainNavigationBar(
                    currentRoute = tabNavigator.current as? TopLevelRoute ?: TopLevelRoute.Library,
                    onSelectRoute = { route ->
                        if (route == TopLevelRoute.Library) {
                            resultEventBus.sendResult(LibrarySearchEvent(""))
                        }
                        tabNavigator.replaceAll(route)
                        if (route == TopLevelRoute.Explore) resultEventBus.sendResult(route.reselectEventKey, Unit)
                    },
                    onReselectRoute = { route ->
                        resultEventBus.sendResult(route.reselectEventKey, Unit)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    ResultEffect<TabEvent> {
        val route = when (it) {
            is TabEvent.Library -> {
                if (it.query != null) resultEventBus.sendResult(LibrarySearchEvent(it.query))
                TopLevelRoute.Library
            }
            TabEvent.Explore -> TopLevelRoute.Explore
            is TabEvent.Browse -> {
                if (it.toExtensions) resultEventBus.sendResult(BrowseShowExtensionsEvent)
                TopLevelRoute.Browse
            }
            is TabEvent.More -> TopLevelRoute.More
        }
        tabNavigator.replaceAll(route)

        if (it is TabEvent.Library) {
            if (it.openLatestChapters) navigator.push(LatestChaptersRoute)
            if (it.mangaIdToOpen != null) navigator.push(MangaRoute(it.mangaIdToOpen))
        }
        if (it is TabEvent.More && it.toDownloads) {
            navigator.push(DownloadQueueRoute)
        }
    }
}

@Composable
private fun NavigationSuiteItem(
    tab: TopLevelRoute,
    tabNavigator: Navigator,
    navigationSuiteType: NavigationSuiteType,
) {
    val resultEventBus = LocalResultEventBus.current
    val selected = tabNavigator.current == tab
    val options = tab.options(selected)

    NavigationSuiteItem(
        navigationSuiteType = navigationSuiteType,
        selected = selected,
        onClick = {
            if (!selected) {
                tabNavigator.replaceAll(tab)
            } else {
                resultEventBus.sendResult(
                    resultKey = tab.reselectEventKey,
                    result = Unit,
                )
            }
        },
        icon = {
            Icon(
                painter = options.icon,
                contentDescription = options.title,
            )
        },
        label = {
            Text(
                text = options.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        badge = tabBadge(tab),
    )
}

@Composable
private fun tabBadge(tab: TopLevelRoute): (@Composable () -> Unit)? {
    val context = LocalContext.current
    val count by produceState(initialValue = 0, tab) {
        val graph = context.appGraph
        when (tab) {
            is TopLevelRoute.Browse -> {
                graph.sourcePreferences.extensionUpdatesCount.changes()
                    .collectLatest { value = it }
            }

            else -> value = 0
        }
    }
    if (count <= 0) return null
    return {
        Badge {
            val desc = when (tab) {
                is TopLevelRoute.Browse -> pluralStringResource(
                    MR.plurals.update_check_notification_ext_updates,
                    count = count,
                    count,
                )

                else -> null
            }
            Text(
                text = count.toString(),
                modifier = Modifier.semantics {
                    if (desc != null) contentDescription = desc
                },
            )
        }
    }
}

sealed interface TabEvent {
    data class Library(
        val mangaIdToOpen: Long? = null,
        val query: String? = null,
        val openLatestChapters: Boolean = false,
    ) : TabEvent
    data object Explore : TabEvent
    data class Browse(val toExtensions: Boolean = false) : TabEvent
    data class More(val toDownloads: Boolean) : TabEvent
}

@Suppress("ConstPropertyName")
private const val TabFadeDuration = 200

data class ShowBottomNavEvent(val visible: Boolean)

data class TabOptions(
    val title: String,
    val icon: Painter,
)

private val spec =
    (fadeIn(tween(TabFadeDuration)) + scaleIn(tween(TabFadeDuration), initialScale = 0.985f)) togetherWith
        (fadeOut(tween(125)) + scaleOut(tween(125), targetScale = 0.99f))

private val TABS: List<TopLevelRoute> = listOf(
    TopLevelRoute.Library,
    TopLevelRoute.Explore,
    TopLevelRoute.More,
)
