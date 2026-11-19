package niaromi.core.security

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import niaromi.core.common.preference.Preference
import niaromi.core.common.preference.PreferenceStore

@Inject
@SingleIn(AppScope::class)
class PrivacyPreferences(
    preferenceStore: PreferenceStore,
) {
    val crashlytics: Preference<Boolean> = preferenceStore.getBoolean("crashlytics", true)

    val analytics: Preference<Boolean> = preferenceStore.getBoolean("analytics", true)
}
