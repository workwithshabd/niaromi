package niaromi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import niaromi.core.archive.archiveReader
import niaromi.core.archive.epubReader
import niaromi.core.common.i18n.stringResource
import niaromi.core.common.util.system.logcat
import niaromi.data.cache.ChapterCache
import niaromi.data.download.DownloadManager
import niaromi.data.download.DownloadProvider
import niaromi.domain.chapter.model.toSChapter
import niaromi.domain.manga.model.Manga
import niaromi.domain.source.model.StubSource
import niaromi.domain.source.service.SourceManager
import niaromi.source.local.LocalSource
import niaromi.source.local.io.Format
import niaromi.ui.reader.model.ReaderChapter
import tachiyomi.i18n.MR

/**
 * Loader used to retrieve the [PageLoader] for a given chapter.
 */
class ChapterLoader(
    private val context: Context,
    private val downloadManager: DownloadManager,
    private val downloadProvider: DownloadProvider,
    private val chapterCache: ChapterCache,
    private val appScope: CoroutineScope,
    private val canLoadAhead: () -> Boolean,
    private val manga: Manga,
    private val source: Source,
    private val sourceManager: SourceManager? = null,
) {

    /**
     * Assigns the chapter's page loader and loads the its pages. Returns immediately if the chapter
     * is already loaded.
     */
    suspend fun loadChapter(chapter: ReaderChapter) {
        if (chapterIsReady(chapter)) {
            return
        }

        chapter.state = ReaderChapter.State.Loading
        withContext(Dispatchers.IO) {
            logcat { "Loading pages for ${chapter.chapter.name}" }
            try {
                val loader = getPageLoader(chapter)
                chapter.pageLoader = loader

                val pages = loader.getPages()
                    .onEach { it.chapter = chapter }

                if (pages.isEmpty()) {
                    throw Exception(context.stringResource(MR.strings.page_list_empty_error))
                }

                // If the chapter is partially read, set the starting page to the last the user read
                // otherwise use the requested page.
                if (!chapter.chapter.read) {
                    chapter.requestedPage = chapter.chapter.lastPageRead
                }

                chapter.state = ReaderChapter.State.Loaded(pages)
            } catch (e: Throwable) {
                chapter.state = ReaderChapter.State.Error(e)
                throw e
            }
        }
    }

    /**
     * Checks [chapter] to be loaded based on present pages and loader in addition to state.
     */
    private fun chapterIsReady(chapter: ReaderChapter): Boolean {
        return chapter.state is ReaderChapter.State.Loaded && chapter.pageLoader != null
    }

    /**
     * Returns the page loader to use for this [chapter].
     */
    private suspend fun getPageLoader(chapter: ReaderChapter): PageLoader {
        val dbChapter = chapter.chapter
        val chapterSourceId = dbChapter.memo["source_id"]?.let {
            try {
                it.toString().removeSurrounding("\"").toLongOrNull()
            } catch (_: Exception) {
                null
            }
        }
        val effectiveSource = if (chapterSourceId != null && sourceManager != null) {
            sourceManager.getOrStub(chapterSourceId)
        } else {
            source
        }
        val isDownloaded = downloadManager.isChapterDownloadedOnDisk(
            dbChapter.name,
            dbChapter.scanlator,
            dbChapter.url,
            manga.title,
            effectiveSource,
        )
        return when {
            isDownloaded -> DownloadPageLoader(
                context,
                chapter,
                manga,
                effectiveSource,
                downloadManager,
                downloadProvider,
            )
            effectiveSource is LocalSource -> effectiveSource.getFormat(chapter.chapter.toSChapter()).let { format ->
                when (format) {
                    is Format.Directory -> DirectoryPageLoader(format.file)
                    is Format.Archive -> ArchivePageLoader(format.file.archiveReader(context))
                    is Format.Epub -> EpubPageLoader(
                        context,
                        format.file.epubReader(context),
                        chapter.chapter.toSChapter(),
                    )
                }
            }
            effectiveSource is HttpSource -> HttpPageLoader(
                chapter,
                effectiveSource,
                chapterCache,
                appScope,
                canLoadAhead,
            )
            effectiveSource is StubSource -> error(
                context.stringResource(MR.strings.source_not_installed, effectiveSource.toString()),
            )
            else -> error(context.stringResource(MR.strings.loader_not_implemented_error))
        }
    }
}
