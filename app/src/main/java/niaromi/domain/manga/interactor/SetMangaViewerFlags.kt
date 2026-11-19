package niaromi.domain.manga.interactor

import dev.zacsweers.metro.Inject
import niaromi.domain.manga.model.MangaUpdate
import niaromi.domain.manga.repository.MangaRepository
import niaromi.ui.reader.setting.ReaderOrientation
import niaromi.ui.reader.setting.ReadingMode

@Inject
class SetMangaViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun awaitSetReadingMode(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.update(
            MangaUpdate(id) {
                viewerFlags = manga.viewerFlags.setFlag(flag, ReadingMode.MASK.toLong())
            },
        )
    }

    suspend fun awaitSetOrientation(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.update(
            MangaUpdate(id) {
                viewerFlags = manga.viewerFlags.setFlag(flag, ReaderOrientation.MASK.toLong())
            },
        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}
