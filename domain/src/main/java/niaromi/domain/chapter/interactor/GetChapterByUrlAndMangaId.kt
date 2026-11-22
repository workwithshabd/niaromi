package niaromi.domain.chapter.interactor

import dev.zacsweers.metro.Inject
import niaromi.domain.chapter.model.Chapter
import niaromi.domain.chapter.repository.ChapterRepository

@Inject
class GetChapterByUrlAndMangaId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(url: String, sourceId: Long): Chapter? {
        return try {
            chapterRepository.getChapterByUrlAndMangaId(url, sourceId)
        } catch (e: Exception) {
            null
        }
    }
}
