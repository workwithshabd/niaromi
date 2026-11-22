package niaromi.data.backup.create.creators

import dev.zacsweers.metro.Inject
import niaromi.data.backup.models.BackupExtensionStore
import niaromi.data.backup.models.backupExtensionStoreMapper
import niaromi.domain.extension.interactor.GetExtensionStores

@Inject
class ExtensionStoresBackupCreator(
    private val getExtensionStores: GetExtensionStores,
) {

    suspend operator fun invoke(): List<BackupExtensionStore> {
        return getExtensionStores.get()
            .map(backupExtensionStoreMapper)
    }
}
