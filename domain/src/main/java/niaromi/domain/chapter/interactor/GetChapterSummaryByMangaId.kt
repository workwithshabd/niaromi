package niaromi.domain.chapter.interactor

import dev.zacsweers.metro.Inject
import niaromi.domain.chapter.model.ChapterSummary
import niaromi.domain.chapter.repository.ChapterRepository

@Inject
class GetChapterSummaryByMangaId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(mangaId: Long): ChapterSummary {
        return chapterRepository.getChapterSummaryByMangaId(mangaId)
    }
}
