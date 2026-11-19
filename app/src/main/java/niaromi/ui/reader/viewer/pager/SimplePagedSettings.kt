package niaromi.ui.reader.viewer.pager

import niaromi.ui.reader.viewer.DualPageSettings

/**
 * Settings only the pager viewers use, all of them part of what the pages are made with.
 */
data class SimplePagedSettings(
    override val dualPageSplit: Boolean,
    override val dualPageInvert: Boolean,
    override val dualPageRotateToFit: Boolean,
    override val dualPageRotateToFitInvert: Boolean,
) : DualPageSettings
