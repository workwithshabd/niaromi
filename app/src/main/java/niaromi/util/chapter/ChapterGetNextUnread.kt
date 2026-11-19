package niaromi.util.chapter

import niaromi.data.download.DownloadManager
import niaromi.domain.chapter.model.Chapter
import niaromi.domain.chapter.model.applyFilters
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
