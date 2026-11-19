package niaromi.ui.reader.viewer.webtoon

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import niaromi.ui.reader.model.ReaderChapter
import niaromi.ui.reader.model.ViewerChapters
import niaromi.ui.reader.viewer.viewerItems

/**
 * Items of a webtoon viewer, where [ViewerChapters] updates are posted.
 */
class WebtoonItems {

    /**
     * List of currently set items.
     */
    var items: List<Any> by mutableStateOf(emptyList())
        private set

    var currentChapter: ReaderChapter? = null
        private set

    /**
     * Updates the items with the given [chapters]. It handles setting a few pages of the
     * next/previous chapter to allow seamless transitions.
     */
    fun setChapters(chapters: ViewerChapters, forceTransition: Boolean) {
        currentChapter = chapters.currChapter
        items = viewerItems(chapters, forceTransition)
    }
}
