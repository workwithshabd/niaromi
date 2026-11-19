package niaromi.domain.chapter.interactor

import dev.zacsweers.metro.Inject
import logcat.LogPriority
import niaromi.core.common.util.system.logcat
import niaromi.domain.chapter.model.Chapter
import niaromi.domain.chapter.repository.ChapterRepository

@Inject
class GetChaptersByMangaId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(mangaId: Long, applyScanlatorFilter: Boolean = false): List<Chapter> {
        return try {
            chapterRepository.getChapterByMangaId(mangaId, applyScanlatorFilter)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
