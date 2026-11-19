package niaromi.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import niaromi.core.common.preference.Preference
import niaromi.core.common.preference.TriState
import niaromi.core.common.preference.getAndSet
import niaromi.data.track.TrackerManager
import niaromi.domain.base.BasePreferences
import niaromi.domain.category.interactor.SetDisplayMode
import niaromi.domain.category.interactor.SetSortModeForCategory
import niaromi.domain.category.model.Category
import niaromi.domain.library.model.LibraryDisplayMode
import niaromi.domain.library.model.LibrarySort
import niaromi.domain.library.service.LibraryPreferences
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class LibrarySettingsViewModel(
    val preferences: BasePreferences,
    val libraryPreferences: LibraryPreferences,
    private val setDisplayMode: SetDisplayMode,
    private val setSortModeForCategory: SetSortModeForCategory,
    trackerManager: TrackerManager,
) : ViewModel() {

    val trackersFlow = trackerManager.loggedInTrackersFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds),
            initialValue = trackerManager.loggedInTrackers(),
        )

    fun toggleFilter(preference: (LibraryPreferences) -> Preference<TriState>) {
        preference(libraryPreferences).getAndSet {
            it.next()
        }
    }

    fun toggleTracker(id: Int) {
        toggleFilter { libraryPreferences.filterTracking(id) }
    }

    fun setDisplayMode(mode: LibraryDisplayMode) {
        setDisplayMode.await(mode)
    }

    fun setSort(category: Category?, mode: LibrarySort.Type, direction: LibrarySort.Direction) {
        viewModelScope.launch {
            setSortModeForCategory.await(category, mode, direction)
        }
    }
}
