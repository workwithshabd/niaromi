package niaromi.domain.chapter.interactor

import dev.zacsweers.metro.Inject
import logcat.LogPriority
import niaromi.core.common.util.system.logcat
import niaromi.domain.chapter.model.Chapter
import niaromi.domain.chapter.repository.ChapterRepository

@Inject
class GetBookmarkedChaptersByMangaId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(mangaId: Long): List<Chapter> {
        return try {
            chapterRepository.getBookmarkedChaptersByMangaId(mangaId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
