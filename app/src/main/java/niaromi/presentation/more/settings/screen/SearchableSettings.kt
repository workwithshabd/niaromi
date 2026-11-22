package niaromi.presentation.more.settings.screen

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.navigation3.runtime.NavKey
import dev.icerock.moko.resources.StringResource
import niaromi.core.navigation.LocalBackButtonVisibility
import niaromi.core.navigation.LocalNavigator
import niaromi.presentation.more.settings.Preference
import niaromi.presentation.more.settings.PreferenceScaffold

interface SearchableSettings : NavKey {

    @Composable
    @ReadOnlyComposable
    fun getTitleRes(): StringResource

    @Composable
    fun getPreferences(): List<Preference>

    @Composable
    fun RowScope.AppBarAction() {
    }

    @Composable
    fun Content() {
        val navigator = LocalNavigator.current
        val backVisibility = LocalBackButtonVisibility.current

        PreferenceScaffold(
            titleRes = getTitleRes(),
            onBackPressed = {
                navigator.pop()
                Unit
            }.takeIf { backVisibility },
            actions = { AppBarAction() },
            itemsProvider = { getPreferences() },
        )
    }

    companion object {
        // HACK: for the background blipping thingy.
        // The title of the target PreferenceItem
        // Set before showing the destination screen and reset after
        // See BasePreferenceWidget.highlightBackground
        var highlightKey: String? = null
    }
}
