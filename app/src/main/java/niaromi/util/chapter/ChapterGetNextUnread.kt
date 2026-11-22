package niaromi.util.chapter

import niaromi.data.download.DownloadManager
import niaromi.domain.chapter.model.Chapter
import niaromi.domain.chapter.model.applyFilters
import niaromi.domain.chapter.service.latestChapterFirst
import niaromi.domain.history.model.History
import niaromi.domain.manga.model.Manga
import niaromi.ui.manga.ChapterList

/**
 * Gets next unread chapter with filters and sorting applied
 */
fun List<Chapter>.getNextUnread(
    manga: Manga,
    downloadedOnly: Boolean,
    downloadManager: DownloadManager,
): Chapter? {
    return applyFilters(manga, downloadedOnly, downloadManager).let { chapters ->
        if (manga.sortDescending()) {
            chapters.findLast { !it.read }
        } else {
            chapters.find { !it.read }
        }
    }
}

/**
 * Gets next unread chapter with filters and sorting applied
 */
fun List<ChapterList.Item>.getNextUnread(manga: Manga, downloadedOnly: Boolean): Chapter? {
    return applyFilters(manga, downloadedOnly).let { chapters ->
        if (manga.sortDescending()) {
            chapters.findLast { !it.chapter.read }
        } else {
            chapters.find { !it.chapter.read }
        }
    }?.chapter
}

fun List<Chapter>.chapterToContinue(history: List<History>): Chapter? {
    val ordered = sortedWith(latestChapterFirst.reversed())
    val lastReadId = history.filter { it.readAt != null }.maxByOrNull { requireNotNull(it.readAt) }?.chapterId
    val lastReadIndex = ordered.indexOfFirst { it.id == lastReadId }
    if (lastReadIndex >= 0) {
        val chapter = ordered[lastReadIndex]
        if (!chapter.read && chapter.lastPageRead > 0) return chapter
        return ordered.getOrNull(lastReadIndex + 1) ?: chapter
    }
    return ordered.firstOrNull { !it.read } ?: ordered.lastOrNull()
}
