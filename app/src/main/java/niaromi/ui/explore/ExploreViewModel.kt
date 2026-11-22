package niaromi.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import niaromi.domain.manga.interactor.GetLibraryManga
import niaromi.domain.manga.interactor.GetManga
import niaromi.domain.manga.interactor.NetworkToLocalManga
import niaromi.domain.manga.model.Manga
import niaromi.domain.manga.model.toDomainManga
import niaromi.domain.source.service.SourceManager
import niaromi.domain.source.service.SourcePreferences
import niaromi.extension.ExtensionManager
import niaromi.source.local.isLocal
import kotlin.math.ln
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

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
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val allItems: List<ExploreItem> = emptyList(),
    val preferredItems: List<ExploreItem> = emptyList(),
    val availableSources: List<SourceFilterItem> = emptyList(),
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
    private val getLibraryManga: GetLibraryManga,
    private val getManga: GetManga,
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreState())
    val state = _state.asStateFlow()

    private var currentPage: Int = 1
    private var rotation = 0
    private var lastRefreshAt = 0L
    private var genreWeights = emptyMap<String, Double>()
    private var readTitles = emptySet<String>()
    private val seenTitles = mutableSetOf<String>()

    private val sourceDispatcher = Dispatchers.IO.limitedParallelism(4)

    init {
        loadExploreFeed()
    }

    fun refreshIfStale() {
        if (android.os.SystemClock.elapsedRealtime() - lastRefreshAt >= 5 * 60_000L) refresh()
    }

    fun refresh() {
        loadExploreFeed(isRefresh = true)
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun loadExploreFeed(isRefresh: Boolean = false) {
        if (isRefresh && (_state.value.isLoading || _state.value.isRefreshing || _state.value.isLoadingMore)) return
        viewModelScope.launch {
            lastRefreshAt = android.os.SystemClock.elapsedRealtime()
            if (isRefresh) {
                _state.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _state.update { it.copy(isLoading = true, errorMessage = null) }
            }

            try {
                rotation = sourcePreferences.exploreRotation.get()
                sourcePreferences.exploreRotation.set((rotation + 1) % 4096)
                val pageToFetch = 1 + (rotation / 2) % 3
                val reading = withContext(Dispatchers.IO) { getLibraryManga.await() }
                val recent = Clock.System.now() - 30.days
                val weights = mutableMapOf<String, Double>()
                reading.filter { it.lastRead != null || it.readCount > 0 }.forEach { item ->
                    val weight = ln(2.0 + item.readCount) * if (item.lastRead?.let { it >= recent } == true) 2 else 1
                    item.manga.genreKeys().forEach { genre -> weights[genre] = (weights[genre] ?: 0.0) + weight }
                }
                genreWeights = weights
                readTitles = reading.map { normalizeTitle(it.manga.title) }.toSet()
                currentPage = pageToFetch
                seenTitles.clear()

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

                if (_state.value.allItems.isEmpty()) {
                    val sourcesById = sortedSources.associateBy { it.id }
                    val cached = withContext(Dispatchers.IO) {
                        sourcePreferences.explorePreviewIds.get().mapNotNull { id ->
                            val manga = getManga.await(id) ?: return@mapNotNull null
                            val source = sourcesById[manga.source] ?: return@mapNotNull null
                            ExploreItem(
                                manga = manga,
                                sourceId = source.id,
                                sourceName = source.name,
                                isPreferredSource =
                                source.id in preferredSourceIds || source.id.toString() in pinnedSources,
                            )
                        }
                    }
                    if (cached.isNotEmpty()) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = true,
                                allItems = cached,
                                preferredItems = cached.filter { item -> item.isPreferredSource },
                            )
                        }
                    }
                }

                // Query sources concurrently
                val rawResults = supervisorScope {
                    sortedSources.map { source ->
                        async(sourceDispatcher) {
                            val isPreferred = source.id in preferredSourceIds || "${source.id}" in pinnedSources
                            try {
                                val mangas = withTimeoutOrNull(8_000) {
                                    suspend fun load(page: Int): List<SManga> {
                                        return if (source.supportsLatest && (rotation + source.id) % 2L == 0L) {
                                            source.getLatestUpdates(page).mangas
                                        } else {
                                            source.getPopularManga(page).mangas
                                        }
                                    }
                                    val page = load(pageToFetch)
                                    if (page.isEmpty() && pageToFetch != 1) load(1) else page
                                }.orEmpty()
                                Triple(source, isPreferred, mangas.take(30))
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                Triple(source, isPreferred, emptyList<SManga>())
                            }
                        }
                    }.awaitAll()
                }

                // Deduplicate across extensions, strictly preserving preferred extension priority
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

                val ranked = rankItems(items, pageToFetch)
                sourcePreferences.explorePreviewIds.set(ranked.take(24).map { it.manga.id })
                val preferred = ranked.filter { it.isPreferredSource }

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
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        hasMorePages = true,
                        allItems = ranked,
                        preferredItems = preferred,
                        availableSources = availableSources,
                        errorMessage = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = "Unable to load explore feed: ${e.message}",
                    )
                }
            }
        }
    }

    fun loadNextPage() {
        val currentVal = _state.value
        if (currentVal.isLoading || currentVal.isRefreshing || currentVal.isLoadingMore || !currentVal.hasMorePages) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            val nextPage = currentPage + 1

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

                val sortedSources = onlineSources.sortedWith(
                    compareByDescending<CatalogueSource> { it.id in preferredSourceIds || "${it.id}" in pinnedSources }
                        .thenByDescending { it.id in preferredSourceIds }
                        .thenBy { it.name.lowercase() },
                )

                val rawResults = supervisorScope {
                    sortedSources.map { source ->
                        async(sourceDispatcher) {
                            val isPreferred = source.id in preferredSourceIds || "${source.id}" in pinnedSources
                            try {
                                val mangas = withTimeoutOrNull(15_000) {
                                    if (source.supportsLatest) {
                                        source.getLatestUpdates(nextPage).mangas
                                    } else {
                                        source.getPopularManga(nextPage).mangas
                                    }
                                } ?: emptyList()
                                Triple(source, isPreferred, mangas.take(30))
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                Triple(source, isPreferred, emptyList<SManga>())
                            }
                        }
                    }.awaitAll()
                }

                val newRawItems = mutableListOf<RawItem>()
                for ((source, isPreferred, mangas) in rawResults) {
                    for (manga in mangas) {
                        val key = normalizeTitle(manga.title)
                        if (key.isNotEmpty() && seenTitles.add(key)) {
                            newRawItems.add(RawItem(manga, source.id, source.name, isPreferred))
                        }
                    }
                }

                if (newRawItems.isEmpty()) {
                    _state.update { it.copy(isLoadingMore = false, hasMorePages = false) }
                    return@launch
                }

                currentPage = nextPage
                val domainMangas = newRawItems.map { it.sManga.toDomainManga(it.sourceId) }
                val localMangas = networkToLocalManga(domainMangas)

                val newExploreItems = newRawItems.zip(localMangas) { raw, local ->
                    ExploreItem(
                        manga = local,
                        sourceId = raw.sourceId,
                        sourceName = raw.sourceName,
                        isPreferredSource = raw.isPreferred,
                    )
                }

                _state.update { current ->
                    val combinedAll = current.allItems + rankItems(newExploreItems, nextPage)
                    current.copy(
                        isLoadingMore = false,
                        allItems = combinedAll,
                        hasMorePages = true,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    private suspend fun rankItems(items: List<ExploreItem>, page: Int): List<ExploreItem> =
        withContext(Dispatchers.Default) {
            val random = Random(rotation * 31 + page)
            items.map { item ->
                val affinity = item.manga.genreKeys().maxOfOrNull { genreWeights[it] ?: 0.0 } ?: 0.0
                val libraryPenalty = if (normalizeTitle(item.manga.title) in readTitles) 3.0 else 0.0
                item to (ln(1 + affinity) + random.nextDouble(from = 0.0, until = 3.0) - libraryPenalty)
            }.sortedByDescending { it.second }.map { it.first }
        }

    private fun Manga.genreKeys(): Set<String> =
        (genre.orEmpty() + listOfNotNull(primaryGenre)).map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

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
