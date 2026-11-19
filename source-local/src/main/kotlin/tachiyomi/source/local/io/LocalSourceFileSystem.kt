package tachiyomi.source.local.io

import com.hippo.unifile.UniFile
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.storage.extension
import tachiyomi.domain.storage.service.StorageManager

@Inject
@SingleIn(AppScope::class)
class LocalSourceFileSystem(
    private val storageManager: StorageManager,
) {

    fun getBaseDirectory(): UniFile? {
        return storageManager.getLocalSourceDirectory()
    }

    fun getFilesInBaseDirectory(): List<UniFile> {
        return getBaseDirectory()?.listFiles().orEmpty().toList()
    }

    fun getMangaDirectory(name: String): UniFile? {
        return getBaseDirectory()
            ?.findFile(name)
            ?.takeIf { it.isDirectory || it.extension.equals("epub", true) || Archive.isSupported(it) }
    }

    fun getFilesInMangaDirectory(name: String): List<UniFile> {
        val file = getBaseDirectory()?.findFile(name) ?: return emptyList()
        return if (file.isDirectory) {
            file.listFiles().orEmpty().toList()
        } else if (file.extension.equals("epub", true) || Archive.isSupported(file)) {
            listOf(file)
        } else {
            emptyList()
        }
    }
}
