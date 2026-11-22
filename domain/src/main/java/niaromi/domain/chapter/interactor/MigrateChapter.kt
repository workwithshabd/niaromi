package niaromi.domain.chapter.interactor

import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.JsonObject
import logcat.LogPriority
import niaromi.core.common.util.system.logcat
import niaromi.domain.chapter.repository.ChapterRepository

@Inject
class MigrateChapter(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(
        chapterId: Long,
        newUrl: String,
        newName: String,
        newScanlator: String?,
        newMemo: JsonObject,
    ) {
        try {
            chapterRepository.migrateChapter(chapterId, newUrl, newName, newScanlator, newMemo)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
