package niaromi.data.backup.create.creators

import dev.zacsweers.metro.Inject
import niaromi.data.backup.models.BackupCategory
import niaromi.data.backup.models.backupCategoryMapper
import niaromi.domain.category.interactor.GetCategories
import niaromi.domain.category.model.Category

@Inject
class CategoriesBackupCreator(
    private val getCategories: GetCategories,
) {

    suspend operator fun invoke(): List<BackupCategory> {
        return getCategories.await()
            .filterNot(Category::isSystemCategory)
            .map(backupCategoryMapper)
    }
}
