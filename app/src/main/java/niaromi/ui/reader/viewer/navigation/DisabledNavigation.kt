package niaromi.ui.reader.viewer.navigation

import niaromi.ui.reader.setting.ReaderPreferences.TappingInvertMode
import niaromi.ui.reader.viewer.ViewerNavigation

/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | M | M | M |   P: Previous
 * +---+---+---+
 * | M | M | M |   M: Menu
 * +---+---+---+
 * | M | M | M |   N: Next
 * +---+---+---+
*/
class DisabledNavigation(
    invertMode: TappingInvertMode,
    isReversed: Boolean,
) : ViewerNavigation(invertMode, isReversed) {

    override val regionList: List<Region> = emptyList()
}
