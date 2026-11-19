package eu.kanade.tachiyomi.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import niaromi.domain.manga.model.toDomainManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.isLocal

data class ExploreItem(
    val manga: Manga,
    val sourceId: Long,
    val sourceName: String,
    val isPreferredSource: Boolean,
)

data class SourceFilterItem(
    val id: Long,
    val name: String,
    val isPreferred: Boolean,
    val count: Int,
)

data class ExploreState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val allItems: List<ExploreItem> = emptyList(),
    val displayedItems: List<ExploreItem> = emptyList(),
    val preferredItems: List<ExploreItem> = emptyList(),
    val availableSources: List<SourceFilterItem> = emptyList(),
    val selectedSourceId: Long? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null,
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class ExploreViewModel(
    private val sourcePreferences: SourcePreferences,
    private val sourceManager: SourceManager,
    private val networkToLocalManga: NetworkToLocalManga,
    private val extensionManager: ExtensionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreState())
    val state = _state.asStateFlow()

    init {
        loadExploreFeed()
    }

    fun refresh() {
        loadExploreFeed(isRefresh = true)
    }

    fun setSearchQuery(query: String) {
        _state.update { current ->
            val filtered = filterItems(current.allItems, current.selectedSourceId, query)
            current.copy(searchQuery = query, displayedItems = filtered)
        }
    }

    fun selectSourceFilter(sourceId: Long?) {
        _state.update { current ->
            val newSelected = if (current.selectedSourceId == sourceId) null else sourceId
            val filtered = filterItems(current.allItems, newSelected, current.searchQuery)
            current.copy(selectedSourceId = newSelected, displayedItems = filtered)
        }
    }

    fun loadExploreFeed(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _state.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _state.update { it.copy(isLoading = true, errorMessage = null) }
            }

            try {
                val enabledLanguages = sourcePreferences.enabledLanguages.get()
                val disabledSources = sourcePreferences.disabledSources.get()
                val pinnedSources = sourcePreferences.pinnedSources.get()
                val preferredExts = sourcePreferences.preferredExtensions.get()
                val loadedExts = extensionManager.getLoadedExtensions()
                val preferredSourceIds = loadedExts
                    .filter { it.pkgName in preferredExts }
                    .flatMap { it.sources }
                    .map { it.id }
                    .toSet()

                val onlineSources = sourceManager.getOnlineSources()
                    .filter { !it.isLocal() }
                    .filter { it.lang in enabledLanguages || it.lang == "all" }
                    .filterNot { it.id.toString() in disabledSources }

                if (onlineSources.isEmpty()) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            allItems = emptyList(),
                            displayedItems = emptyList(),
                            preferredItems = emptyList(),
                            availableSources = emptyList(),
                            errorMessage = "No extensions installed or enabled. " +
                                "Install extensions to explore new titles.",
                        )
                    }
                    return@launch
                }

                // Preferred extensions and pinned sources come FIRST
                val sortedSources = onlineSources.sortedWith(
                    compareByDescending<CatalogueSource> { it.id in preferredSourceIds || "${it.id}" in pinnedSources }
                        .thenByDescending { it.id in preferredSourceIds }
                        .thenBy { it.name.lowercase() },
                )

                // Query sources concurrently
                val rawResults = supervisorScope {
                    sortedSources.map { source ->
                        async(Dispatchers.IO) {
                            val isPreferred = source.id in preferredSourceIds || "${source.id}" in pinnedSources
                            try {
                                val mangas = withTimeoutOrNull(15_000) {
                                    if (source.supportsLatest) {
                                        source.getLatestUpdates(1).mangas
                                    } else {
                                        source.getPopularManga(1).mangas
                                    }
                                } ?: emptyList()
                                Triple(source, isPreferred, mangas)
                            } catch (e: Exception) {
                                Triple(source, isPreferred, emptyList<SManga>())
                            }
                        }
                    }.awaitAll()
                }

                // Deduplicate across extensions, strictly preserving preferred extension priority
                val seenTitles = mutableSetOf<String>()
                val rawExploreItems = mutableListOf<RawItem>()

                for ((source, isPreferred, mangas) in rawResults) {
                    for (manga in mangas) {
                        val key = normalizeTitle(manga.title)
                        if (key.isNotEmpty() && seenTitles.add(key)) {
                            rawExploreItems.add(RawItem(manga, source.id, source.name, isPreferred))
                        }
                    }
                }

                if (rawExploreItems.isEmpty()) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            allItems = emptyList(),
                            displayedItems = emptyList(),
                            preferredItems = emptyList(),
                            availableSources = emptyList(),
                            errorMessage = "No new titles found across enabled extensions.",
                        )
                    }
                    return@launch
                }

                // Convert and persist network manga
                val domainMangas = rawExploreItems.map { it.sManga.toDomainManga(it.sourceId) }
                val localMangas = networkToLocalManga(domainMangas)

                val items = rawExploreItems.zip(localMangas) { raw, local ->
                    ExploreItem(
                        manga = local,
                        sourceId = raw.sourceId,
                        sourceName = raw.sourceName,
                        isPreferredSource = raw.isPreferred,
                    )
                }

                val preferred = items.filter { it.isPreferredSource }

                val sourceCounts = items.groupBy { it.sourceId }
                val availableSources = sortedSources.mapNotNull { source ->
                    val count = sourceCounts[source.id]?.size ?: 0
                    if (count > 0) {
                        SourceFilterItem(
                            id = source.id,
                            name = source.name,
                            isPreferred = "${source.id}" in pinnedSources,
                            count = count,
                        )
                    } else {
                        null
                    }
                }

                _state.update { current ->
                    val filtered = filterItems(items, current.selectedSourceId, current.searchQuery)
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        allItems = items,
                        displayedItems = filtered,
                        preferredItems = preferred,
                        availableSources = availableSources,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Unable to load explore feed: ${e.message}",
                    )
                }
            }
        }
    }

    private fun filterItems(
        items: List<ExploreItem>,
        sourceId: Long?,
        query: String,
    ): List<ExploreItem> {
        return items.filter { item ->
            val matchesSource = sourceId == null || item.sourceId == sourceId
            val matchesQuery = query.isBlank() ||
                item.manga.title.contains(query, ignoreCase = true) ||
                (item.manga.author?.contains(query, ignoreCase = true) == true) ||
                (item.manga.genre?.any { it.contains(query, ignoreCase = true) } == true)
            matchesSource && matchesQuery
        }
    }

    companion object {
        fun normalizeTitle(title: String): String {
            return title.lowercase()
                .replace(Regex("""\[.*?\]|\(.*?\)|<.*?>"""), " ")
                .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
        }
    }

    private data class RawItem(
        val sManga: SManga,
        val sourceId: Long,
        val sourceName: String,
        val isPreferred: Boolean,
    )
}
