package niaromi.domain.category.interactor

import dev.zacsweers.metro.Inject
import niaromi.domain.library.model.LibraryDisplayMode
import niaromi.domain.library.service.LibraryPreferences

@Inject
class SetDisplayMode(
    private val preferences: LibraryPreferences,
) {

    fun await(display: LibraryDisplayMode) {
        preferences.displayMode.set(display)
    }
}
