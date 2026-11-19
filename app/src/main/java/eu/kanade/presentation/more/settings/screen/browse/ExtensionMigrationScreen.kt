package eu.kanade.presentation.more.settings.screen.browse

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.extension.extensionsTab
import eu.kanade.tachiyomi.ui.browse.migration.sources.migrateSourceTab
import niaromi.core.navigation.LocalNavigator
import tachiyomi.i18n.MR

@Composable
fun ExtensionMigrationScreen(
    initialTab: Int = 0,
) {
    val navigator = LocalNavigator.current
    val extensionsViewModel = metroViewModel<ExtensionsViewModel>()
    val extensionsSearchQuery by extensionsViewModel.searchQuery.collectAsStateWithLifecycle()

    val tabs = listOf(
        extensionsTab(extensionsViewModel),
        migrateSourceTab(),
    )

    val pagerState = rememberPagerState(initialPage = initialTab) { tabs.size }

    TabbedScreen(
        titleRes = MR.strings.label_extensions,
        tabs = tabs,
        state = pagerState,
        searchQuery = extensionsSearchQuery,
        onChangeSearchQuery = extensionsViewModel::search,
        navigateUp = navigator::pop,
    )
}
