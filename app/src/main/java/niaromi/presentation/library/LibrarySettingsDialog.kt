package niaromi.presentation.library

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import niaromi.core.common.preference.TriState
import niaromi.domain.library.model.DiscoverySort
import niaromi.domain.library.model.LibraryDisplayMode
import niaromi.domain.library.model.sort
import niaromi.domain.library.service.LibraryPreferences
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.presentation.components.RadioMenuItem
import niaromi.presentation.components.TabbedDialogPaddings
import niaromi.presentation.core.components.CheckboxItem
import niaromi.presentation.core.components.HeadingItem
import niaromi.presentation.core.components.SettingsChipRow
import niaromi.presentation.core.components.SliderItem
import niaromi.presentation.core.components.TriStateItem
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.core.util.collectAsState
import niaromi.ui.library.LibrarySettingsViewModel
import niaromi.util.system.isReleaseBuildType
import tachiyomi.i18n.MR

@Composable
fun LibrarySettingsDialog(
    onDismissRequest: () -> Unit,
    viewModel: LibrarySettingsViewModel,
) {
    val tabs = listOf(
        stringResource(MR.strings.action_filter),
        stringResource(MR.strings.action_sort),
    )
    val pager = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val optionsHeight = LocalConfiguration.current.screenHeightDp.dp * 0.45f
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
                Text(
                    text = "Library options",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                )
                PrimaryTabRow(
                    selectedTabIndex = pager.currentPage,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pager.currentPage == index,
                            onClick = { scope.launch { pager.animateScrollToPage(index) } },
                            text = { Text(title) },
                        )
                    }
                }
                HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(optionsHeight)) { page ->
                    Column(
                        Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = TabbedDialogPaddings.Vertical),
                    ) {
                        when (page) {
                            0 -> FilterPage(viewModel)
                            1 -> SortPage(viewModel)
                        }
                    }
                }
                TextButton(onDismissRequest, Modifier.align(Alignment.End).padding(horizontal = 16.dp)) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.FilterPage(
    viewModel: LibrarySettingsViewModel,
) {
    val filterDownloaded by viewModel.libraryPreferences.filterDownloaded.collectAsState()
    val downloadedOnly by viewModel.preferences.downloadedOnly.collectAsState()
    val autoUpdateMangaRestrictions by viewModel.libraryPreferences.autoUpdateMangaRestrictions.collectAsState()

    TriStateItem(
        label = stringResource(MR.strings.label_downloaded),
        state = if (downloadedOnly) {
            TriState.ENABLED_IS
        } else {
            filterDownloaded
        },
        enabled = !downloadedOnly,
        onClick = { viewModel.toggleFilter(LibraryPreferences::filterDownloaded) },
    )
    val filterUnread by viewModel.libraryPreferences.filterUnread.collectAsState()
    TriStateItem(
        label = stringResource(MR.strings.action_filter_unread),
        state = filterUnread,
        onClick = { viewModel.toggleFilter(LibraryPreferences::filterUnread) },
    )
    val filterStarted by viewModel.libraryPreferences.filterStarted.collectAsState()
    TriStateItem(
        label = stringResource(MR.strings.label_started),
        state = filterStarted,
        onClick = { viewModel.toggleFilter(LibraryPreferences::filterStarted) },
    )
    val filterBookmarked by viewModel.libraryPreferences.filterBookmarked.collectAsState()
    TriStateItem(
        label = stringResource(MR.strings.action_filter_bookmarked),
        state = filterBookmarked,
        onClick = { viewModel.toggleFilter(LibraryPreferences::filterBookmarked) },
    )
    val filterCompleted by viewModel.libraryPreferences.filterCompleted.collectAsState()
    TriStateItem(
        label = stringResource(MR.strings.completed),
        state = filterCompleted,
        onClick = { viewModel.toggleFilter(LibraryPreferences::filterCompleted) },
    )
    // TODO: re-enable when custom intervals are ready for stable
    if ((!isReleaseBuildType) && LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD in autoUpdateMangaRestrictions) {
        val filterIntervalCustom by viewModel.libraryPreferences.filterIntervalCustom.collectAsState()
        TriStateItem(
            label = stringResource(MR.strings.action_filter_interval_custom),
            state = filterIntervalCustom,
            onClick = { viewModel.toggleFilter(LibraryPreferences::filterIntervalCustom) },
        )
    }

    val trackers by viewModel.trackersFlow.collectAsState()
    when (trackers.size) {
        0 -> {
            // No trackers
        }
        1 -> {
            val service = trackers[0]
            val filterTracker by viewModel.libraryPreferences.filterTracking(service.id.toInt()).collectAsState()
            TriStateItem(
                label = stringResource(MR.strings.action_filter_tracked),
                state = filterTracker,
                onClick = { viewModel.toggleTracker(service.id.toInt()) },
            )
        }
        else -> {
            HeadingItem(MR.strings.action_filter_tracked)
            trackers.map { service ->
                val filterTracker by viewModel.libraryPreferences.filterTracking(service.id.toInt()).collectAsState()
                TriStateItem(
                    label = service.name,
                    state = filterTracker,
                    onClick = { viewModel.toggleTracker(service.id.toInt()) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SortPage(
    viewModel: LibrarySettingsViewModel,
) {
    val preference = viewModel.libraryPreferences.discoverySort
    val selected by preference.collectAsState()
    DiscoverySort.entries.forEach { sort ->
        RadioMenuItem(text = { Text(sort.label) }, isChecked = sort == selected, onClick = { preference.set(sort) })
    }
}
