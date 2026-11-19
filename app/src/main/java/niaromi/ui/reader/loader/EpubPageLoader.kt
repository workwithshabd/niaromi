package niaromi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import niaromi.core.archive.EpubReader
import niaromi.ui.reader.model.ReaderPage

/**
 * Loader used to load a chapter from a .epub file.
 */
internal class EpubPageLoader(
    private val context: Context,
    private val reader: EpubReader,
    private val chapter: SChapter? = null,
) : PageLoader() {

    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderPage> {
        if (!reader.isTextEbook()) {
            return reader.getImagesFromPages().mapIndexed { i, path ->
                ReaderPage(i).apply {
                    stream = { reader.getInputStream(path)!! }
                    status = Page.State.Ready
                }
            }
        }

        val spineHrefs = reader.getSpineHrefs()
        val allChapters = reader.getChapters()
        val url = chapter?.url.orEmpty()
        val chapterSpines = if (url.contains('#')) {
            val parts = url.split('#')
            val spineIdx = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val targetHref = parts.getOrNull(2).orEmpty()
            val currIdx = allChapters.indexOfFirst { it.href == targetHref || it.spineIndex == spineIdx }
            if (currIdx >= 0) {
                val nextSpineIdx = if (currIdx <
                    allChapters.size - 1
                ) {
                    allChapters[currIdx + 1].spineIndex
                } else {
                    spineHrefs.size
                }
                spineHrefs.subList(
                    spineIdx.coerceIn(0, spineHrefs.size),
                    nextSpineIdx.coerceIn(spineIdx, spineHrefs.size),
                ).ifEmpty { listOf(targetHref).filter(String::isNotBlank) }
            } else if (targetHref.isNotBlank()) {
                listOf(targetHref)
            } else {
                spineHrefs
            }
        } else {
            spineHrefs
        }

        val chapterTitle = chapter?.name.orEmpty()
        val renderer = EbookPageRenderer(context, reader, chapterTitle, "")
        val pageItems = renderer.buildPages(chapterSpines)

        return pageItems.mapIndexed { index, item ->
            ReaderPage(index).apply {
                stream = { renderer.renderToStream(item) }
                status = Page.State.Ready
            }
        }
    }

    override suspend fun loadPage(page: ReaderPage) {
        check(!isRecycled)
    }

    override fun recycle() {
        super.recycle()
        reader.close()
    }
}
