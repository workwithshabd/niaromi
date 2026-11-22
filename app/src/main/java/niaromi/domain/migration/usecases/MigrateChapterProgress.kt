package niaromi.domain.migration.usecases

import niaromi.domain.chapter.model.Chapter
import niaromi.domain.chapter.model.ChapterUpdate

internal fun migrateChapterProgress(previous: List<Chapter>, target: List<Chapter>): List<ChapterUpdate> {
    val previousByChapter = previous
        .sortedByDescending(Chapter::read)
        .groupBy(Chapter::migrationKey)
        .mapValues { (_, chapters) -> ArrayDeque(chapters) }

    return target.sortedByDescending(Chapter::read).mapNotNull { chapter ->
        val match = previousByChapter[chapter.migrationKey()]?.removeFirstOrNull() ?: return@mapNotNull null
        ChapterUpdate(chapter.id) {
            if (match.read && !chapter.read) read = true
            if (match.bookmark && !chapter.bookmark) bookmark = true
            dateFetch = match.dateFetch
        }
    }
}

private fun Chapter.migrationKey(): String {
    return if (isRecognizedNumber) "number:$chapterNumber" else "title:${name.trim().lowercase()}"
}
