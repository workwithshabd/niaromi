package niaromi.ui.browse.migration.sources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.MigrateMangaRoute
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.automirroredrounded.Help
import niaromi.presentation.browse.MigrateSourceScreen
import niaromi.presentation.components.AppBar
import niaromi.presentation.components.TabContent
import niaromi.presentation.core.i18n.stringResource
import tachiyomi.i18n.MR

@Composable
fun migrateSourceTab(): TabContent {
    val uriHandler = LocalUriHandler.current
    val navigator = LocalNavigator.current
    val viewModel = metroViewModel<MigrateSourceViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    return TabContent(
        titleRes = MR.strings.label_migration,
        actions = listOf(
            AppBar.Action(
                title = stringResource(MR.strings.migration_help_guide),
                icon = MaterialSymbols.AutoMirroredRounded.Help,
                onClick = {
                    uriHandler.openUri("https://github.com/workwithshabd/niaromi")
                },
            ),
        ),
        content = { contentPadding, _ ->
            MigrateSourceScreen(
                state = state,
                contentPadding = contentPadding,
                onClickItem = { source ->
                    navigator.push(MigrateMangaRoute(source.id))
                },
                onToggleSortingDirection = viewModel::toggleSortingDirection,
                onToggleSortingMode = viewModel::toggleSortingMode,
            )
        },
    )
}
