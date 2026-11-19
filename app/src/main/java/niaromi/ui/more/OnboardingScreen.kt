package niaromi.ui.more

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import niaromi.app.di.appGraph
import niaromi.core.navigation.LocalNavigator
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.core.util.collectAsState
import niaromi.presentation.core.util.isTabletUi
import niaromi.presentation.more.onboarding.OnboardingScreen
import niaromi.presentation.more.settings.screen.SearchableSettings
import niaromi.presentation.more.settings.screen.SettingsDataScreen
import niaromi.presentation.more.settings.screen.openSettings

@Composable
fun OnboardingScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val isTabletUi = isTabletUi()

    val basePreferences = remember { context.appGraph.basePreferences }
    val shownOnboardingFlow by basePreferences.shownOnboardingFlow.collectAsState()

    val finishOnboarding: () -> Unit = {
        basePreferences.shownOnboardingFlow.set(true)
        navigator.pop()
    }

    val restoreSettingKey = stringResource(SettingsDataScreen.restorePreferenceKeyString)

    BackHandler(enabled = !shownOnboardingFlow) {
        // Prevent exiting if onboarding hasn't been completed
    }

    OnboardingScreen(
        onComplete = finishOnboarding,
        onRestoreBackup = {
            finishOnboarding()
            SearchableSettings.highlightKey = restoreSettingKey
            navigator.openSettings(SettingsDataScreen, isTabletUi)
        },
    )
}
