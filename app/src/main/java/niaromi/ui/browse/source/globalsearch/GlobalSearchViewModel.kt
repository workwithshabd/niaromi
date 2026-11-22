package niaromi.ui.browse.source.globalsearch

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.tachiyomi.source.Source
import niaromi.domain.manga.interactor.GetManga
import niaromi.domain.manga.interactor.NetworkToLocalManga
import niaromi.domain.source.service.SourceManager
import niaromi.domain.source.service.SourcePreferences
import niaromi.extension.ExtensionManager

@AssistedInject
class GlobalSearchViewModel(
    @Assisted initialQuery: String,
    @Assisted initialExtensionFilter: String?,
    @Assisted searchAllSources: Boolean,
    sourcePreferences: SourcePreferences,
    sourceManager: SourceManager,
    extensionManager: ExtensionManager,
    networkToLocalManga: NetworkToLocalManga,
    getManga: GetManga,
    preferences: SourcePreferences,
) : SearchViewModel(
    initialState = State(
        searchQuery = initialQuery,
        sourceFilter = if (
            searchAllSources || initialExtensionFilter != null || sourcePreferences.preferredExtensions.get().isEmpty()
        ) {
            SourceFilter.All
        } else {
            SourceFilter.Preferred
        },
    ),
    sourcePreferences = sourcePreferences,
    sourceManager = sourceManager,
    extensionManager = extensionManager,
    networkToLocalManga = networkToLocalManga,
    getManga = getManga,
    preferences = preferences,
) {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(
            initialQuery: String,
            initialExtensionFilter: String?,
            searchAllSources: Boolean,
        ): GlobalSearchViewModel
    }

    init {
        extensionFilter = initialExtensionFilter
        search()
    }

    override suspend fun getEnabledSources(): List<Source> {
        val sources = super.getEnabledSources()
        return when (state.value.sourceFilter) {
            SourceFilter.All -> sources
            SourceFilter.Preferred -> sources.filter { it.id in preferredSourceIds }
            SourceFilter.PinnedOnly -> sources.filter { it.id.toString() in pinnedSources }
        }
    }
}
