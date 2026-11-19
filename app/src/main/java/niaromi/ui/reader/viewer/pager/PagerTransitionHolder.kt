package niaromi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import niaromi.ui.reader.model.ChapterTransition
import niaromi.ui.reader.viewer.ReaderTransitionView
import niaromi.ui.reader.viewer.transitionDownloadState
import niaromi.util.system.dpToPx
import niaromi.widget.ViewPagerAdapter

/**
 * View of the ViewPager that contains a chapter transition.
 */
@SuppressLint("ViewConstructor")
class PagerTransitionHolder(
    readerThemedContext: Context,
    val viewer: PagerViewer,
    val transition: ChapterTransition,
) : LinearLayout(readerThemedContext), ViewPagerAdapter.PositionableView {

    /**
     * Item that identifies this view. Needed by the adapter to not recreate views.
     */
    override val item: Any
        get() = transition

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val sidePadding = 64.dpToPx
        setPadding(sidePadding, 0, sidePadding, 0)

        val transitionView = ReaderTransitionView(context)
        addView(transitionView)

        transitionView.bind(
            transition = transition,
            downloadState = viewer.downloadManager.transitionDownloadState(
                transition,
                viewer.activity.viewModel.manga,
                viewer.activity.viewModel.source,
            ),
            onRetry = viewer.activity::requestPreloadChapter,
            onButtonPressChange = { pressed -> viewer.pager.setGestureDetectorEnabled(!pressed) },
        )
    }
}
