package niaromi.domain.source.interactor

import dev.zacsweers.metro.Inject
import niaromi.core.common.preference.getAndSet
import niaromi.domain.source.model.Source
import niaromi.domain.source.service.SourcePreferences

@Inject
class ToggleSourcePin(
    private val preferences: SourcePreferences,
) {

    fun await(source: Source) {
        val isPinned = source.id.toString() in preferences.pinnedSources.get()
        preferences.pinnedSources.getAndSet { pinned ->
            if (isPinned) pinned.minus("${source.id}") else pinned.plus("${source.id}")
        }
    }
}
