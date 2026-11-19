package niaromi.domain.manga.interactor

import dev.zacsweers.metro.Inject
import niaromi.domain.manga.model.MangaUpdate
import niaromi.domain.manga.repository.MangaRepository

@Inject
class UpdateMangaNotes(
    private val mangaRepository: MangaRepository,
) {

    suspend operator fun invoke(mangaId: Long, notes: String): Boolean {
        return mangaRepository.update(
            MangaUpdate(mangaId) {
                this.notes = notes
            },
        )
    }
}
