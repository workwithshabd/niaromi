package niaromi.domain.manga.interactor

import dev.zacsweers.metro.Inject
import niaromi.domain.manga.model.Manga
import niaromi.domain.manga.model.MangaWithChapterCount
import niaromi.domain.manga.repository.MangaRepository

@Inject
class GetDuplicateLibraryManga(
    private val mangaRepository: MangaRepository,
) {

    suspend operator fun invoke(manga: Manga): List<MangaWithChapterCount> {
        return mangaRepository.getDuplicateLibraryManga(manga.id, manga.title.lowercase())
    }
}
