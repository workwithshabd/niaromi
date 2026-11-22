package niaromi.domain.category.interactor

import dev.zacsweers.metro.Inject
import niaromi.domain.category.repository.CategoryRepository
import niaromi.domain.library.model.plus
import niaromi.domain.library.service.LibraryPreferences

@Inject
class ResetCategoryFlags(
    private val preferences: LibraryPreferences,
    private val categoryRepository: CategoryRepository,
) {

    suspend fun await() {
        val sort = preferences.sortingMode.get()
        categoryRepository.updateAllFlags(sort.type + sort.direction)
    }
}
