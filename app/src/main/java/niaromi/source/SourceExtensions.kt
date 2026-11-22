package niaromi.source

import eu.kanade.tachiyomi.source.Source
import niaromi.domain.source.model.StubSource
import niaromi.domain.source.service.SourcePreferences
import niaromi.source.local.isLocal

fun Source.getNameForMangaInfo(preferences: SourcePreferences): String {
    val enabledLanguages = preferences.enabledLanguages.get()
        .filterNot { it in listOf("all", "other") }
    val hasOneActiveLanguages = enabledLanguages.size == 1
    val isInEnabledLanguages = lang in enabledLanguages
    return when {
        // For edge cases where user disables a source they got manga of in their library.
        hasOneActiveLanguages && !isInEnabledLanguages -> toString()
        // Hide the language tag when only one language is used.
        hasOneActiveLanguages && isInEnabledLanguages -> name
        else -> toString()
    }
}

fun Source.isLocalOrStub(): Boolean = isLocal() || this is StubSource
