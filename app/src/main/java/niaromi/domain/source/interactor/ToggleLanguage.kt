package niaromi.domain.source.interactor

import dev.zacsweers.metro.Inject
import niaromi.core.common.preference.getAndSet
import niaromi.domain.source.service.SourcePreferences

@Inject
class ToggleLanguage(
    val preferences: SourcePreferences,
) {

    fun await(language: String) {
        val isEnabled = language in preferences.enabledLanguages.get()
        preferences.enabledLanguages.getAndSet { enabled ->
            if (isEnabled) enabled.minus(language) else enabled.plus(language)
        }
    }
}
