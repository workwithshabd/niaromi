package niaromi.ui.browse.extension

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.icerock.moko.resources.StringResource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import niaromi.domain.base.BasePreferences
import niaromi.domain.extension.interactor.GetExtensionsByType
import niaromi.domain.source.service.SourcePreferences
import niaromi.extension.ExtensionManager
import niaromi.extension.model.Extension
import niaromi.extension.model.InstallStep
import niaromi.util.system.LocaleHelper
import tachiyomi.i18n.MR
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class ExtensionsViewModel(
    private val preferences: SourcePreferences,
    basePreferences: BasePreferences,
    private val extensionManager: ExtensionManager,
    private val getExtensions: GetExtensionsByType,
    private val context: Context,
) : ViewModel() {

    private val currentDownloads = MutableStateFlow<Map<String, InstallStep>>(hashMapOf())

    val searchQuery: StateFlow<String?>
        field = MutableStateFlow(null)

    // Public so the tab badge can observe it without subscribing to the whole state.
    val updatesCount = preferences.extensionUpdatesCount.changes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), 0)

    private val isRefreshing = MutableStateFlow(false)

    private fun extensionMapper(map: Map<String, InstallStep>): (Extension) -> ExtensionUiModel.Item = {
        ExtensionUiModel.Item(it, map[it.pkgName] ?: InstallStep.Idle)
    }

    @Suppress("LocalVariableName")
    private val items = combine(
        searchQuery
            // No query is the common case on every open, so don't delay it
            .debounce { if (it.isNullOrEmpty()) Duration.ZERO else 0.25.seconds }
            .map { searchQueryPredicate(it ?: "") },
        currentDownloads,
        getExtensions.subscribe(),
        preferences.preferredExtensions.changes(),
    ) { predicate, downloads, (_updates, _loaded, _available, _notLoaded), preferredExts ->
        buildMap {
            val updates = _updates.filter(predicate).map(extensionMapper(downloads))
            if (updates.isNotEmpty()) {
                put(ExtensionUiModel.Header.Resource(MR.strings.ext_updates_pending), updates)
            }

            val notLoaded = _notLoaded.filter(predicate).map(extensionMapper(downloads))
            if (notLoaded.isNotEmpty()) {
                put(ExtensionUiModel.Header.Resource(MR.strings.ext_not_loaded), notLoaded)
            }

            val loaded = _loaded
                .filter(predicate)
                .sortedWith(
                    compareByDescending<Extension> { it.pkgName in preferredExts }
                        .thenBy { it.name.lowercase() },
                )
                .map(extensionMapper(downloads))
            if (loaded.isNotEmpty()) {
                put(ExtensionUiModel.Header.Resource(MR.strings.ext_installed), loaded)
            }

            val languagesWithExtensions = _available
                .filter(predicate)
                .groupBy { it.lang }
                .toSortedMap(LocaleHelper.comparator)
                .map { (lang, exts) ->
                    ExtensionUiModel.Header.Text(LocaleHelper.getSourceDisplayName(lang, context)) to
                        exts.map(extensionMapper(downloads))
                }
            if (languagesWithExtensions.isNotEmpty()) {
                putAll(languagesWithExtensions)
            }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)

    val state: StateFlow<State> = combine(
        items,
        searchQuery,
        isRefreshing,
        preferences.extensionUpdatesCount.changes(),
        combine(
            basePreferences.extensionInstaller.changes(),
            preferences.preferredExtensions.changes(),
            ::Pair,
        ),
    ) { items, searchQuery, isRefreshing, updates, (installer, preferredExtensions) ->
        State(
            isLoading = items == null,
            isRefreshing = isRefreshing,
            items = items.orEmpty(),
            updates = updates,
            installer = installer,
            searchQuery = searchQuery,
            preferredExtensions = preferredExtensions,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), State())

    init {

        viewModelScope.launch(Dispatchers.IO) { findAvailableExtensions() }
    }

    fun searchQueryPredicate(query: String): (Extension) -> Boolean {
        val subqueries = query.split(",")
            .map { it.trim() }
            .filterNot { it.isBlank() }

        if (subqueries.isEmpty()) return { true }

        return { extension ->
            subqueries.any { subquery ->
                if (extension.name.contains(subquery, ignoreCase = true)) return@any true

                when (extension) {
                    is Extension.Loaded -> extension.sources.any { source ->
                        source.name.contains(subquery, ignoreCase = true) ||
                            (source as? HttpSource)?.getHomeUrl()?.contains(subquery, ignoreCase = true) == true ||
                            source.id == subquery.toLongOrNull()
                    }

                    is Extension.Available -> extension.sources.any {
                        it.name.contains(subquery, ignoreCase = true) ||
                            it.baseUrl.contains(subquery, ignoreCase = true) ||
                            it.id == subquery.toLongOrNull()
                    }

                    else -> false
                }
            }
        }
    }

    fun search(query: String?) {
        searchQuery.update { query }
    }

    fun updateAllExtensions() {
        viewModelScope.launch(Dispatchers.IO) {
            state.value.items.values.flatten()
                .map { it.extension }
                .filterIsInstance<Extension.Installed>()
                .filter { it.hasUpdate }
                .forEach(::updateExtension)
        }
    }

    fun installExtension(extension: Extension.Available) {
        viewModelScope.launch(Dispatchers.IO) {
            extensionManager.installExtension(extension).collectToInstallUpdate(extension)
        }
    }

    fun updateExtension(extension: Extension.Installed) {
        viewModelScope.launch(Dispatchers.IO) {
            extensionManager.updateExtension(extension).collectToInstallUpdate(extension)
        }
    }

    fun cancelInstallUpdateExtension(extension: Extension) {
        extensionManager.cancelInstallUpdateExtension(extension)
        removeDownloadState(extension)
    }

    private fun addDownloadState(extension: Extension, installStep: InstallStep) {
        currentDownloads.update { it + Pair(extension.pkgName, installStep) }
    }

    private fun removeDownloadState(extension: Extension) {
        currentDownloads.update { it - extension.pkgName }
    }

    private suspend fun Flow<InstallStep>.collectToInstallUpdate(extension: Extension) =
        this
            .onEach { installStep -> addDownloadState(extension, installStep) }
            .takeWhile { installStep -> installStep != InstallStep.Installed }
            .onCompletion { removeDownloadState(extension) }
            .collect()

    fun uninstallExtension(extension: Extension.Installed) {
        val preferred = preferences.preferredExtensions.get()
        if (extension.pkgName in preferred) {
            preferences.preferredExtensions.set(preferred - extension.pkgName)
        }
        extensionManager.uninstallExtension(extension)
    }

    fun togglePreferredExtension(extension: Extension) {
        val current = preferences.preferredExtensions.get()
        val isPreferred = extension.pkgName in current
        if (isPreferred) {
            preferences.preferredExtensions.set(current - extension.pkgName)
        } else {
            preferences.preferredExtensions.set(current + extension.pkgName)
        }
        if (extension is Extension.Loaded) {
            val currentPinned = preferences.pinnedSources.get()
            val sourceIds = extension.sources.map { it.id.toString() }.toSet()
            if (isPreferred) {
                preferences.pinnedSources.set(currentPinned - sourceIds)
            } else {
                preferences.pinnedSources.set(currentPinned + sourceIds)
            }
        }
    }

    fun findAvailableExtensions() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.update { true }

            extensionManager.findAvailableExtensions()

            // Fake slower refresh so it doesn't seem like it's not doing anything
            delay(1.seconds)

            isRefreshing.update { false }
        }
    }

    fun trustExtension(extension: Extension.NotLoaded) {
        extensionManager.trust(extension)
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val items: ItemGroups = mutableMapOf(),
        val updates: Int = 0,
        val installer: BasePreferences.ExtensionInstaller? = null,
        val searchQuery: String? = null,
        val preferredExtensions: Set<String> = emptySet(),
    ) {
        val isEmpty = items.isEmpty()
    }
}

typealias ItemGroups = Map<ExtensionUiModel.Header, List<ExtensionUiModel.Item>>

object ExtensionUiModel {
    sealed interface Header {
        data class Resource(val textRes: StringResource) : Header
        data class Text(val text: String) : Header
    }

    data class Item(
        val extension: Extension,
        val installStep: InstallStep,
    )
}
