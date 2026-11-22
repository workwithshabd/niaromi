package niaromi.domain.download.interactor

import dev.zacsweers.metro.Inject
import niaromi.data.download.DownloadManager
import niaromi.domain.chapter.model.Chapter
import niaromi.domain.manga.model.Manga
import niaromi.domain.source.service.SourceManager

@Inject
class DeleteDownload(
    private val sourceManager: SourceManager,
    private val downloadManager: DownloadManager,
) {

    suspend fun awaitAll(manga: Manga, vararg chapters: Chapter) {
        sourceManager.get(manga.source)?.let { source ->
            downloadManager.deleteChapters(chapters.toList(), manga, source)
        }
    }
}
