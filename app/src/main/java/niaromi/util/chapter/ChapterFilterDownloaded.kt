package niaromi.util.chapter

import niaromi.data.download.DownloadCache
import niaromi.domain.chapter.model.Chapter
import niaromi.domain.manga.model.Manga
import niaromi.source.local.isLocal

/**
 * Returns a copy of the list with not downloaded chapters removed.
 */
fun List<Chapter>.filterDownloaded(manga: Manga, downloadCache: DownloadCache): List<Chapter> {
    if (manga.isLocal()) return this

    return filter { downloadCache.isChapterDownloaded(it.name, it.scanlator, it.url, manga.title, manga.source) }
}
