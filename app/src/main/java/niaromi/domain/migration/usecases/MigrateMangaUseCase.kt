package niaromi.domain.migration.usecases

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import niaromi.core.metro.AppCoroutineScope
import niaromi.data.cache.CoverCache
import niaromi.data.download.DownloadManager
import niaromi.data.track.EnhancedTracker
import niaromi.data.track.TrackerManager
import niaromi.domain.category.interactor.GetCategories
import niaromi.domain.category.interactor.SetMangaCategories
import niaromi.domain.chapter.interactor.GetChaptersByMangaId
import niaromi.domain.chapter.interactor.UpdateChapter
import niaromi.domain.chapter.model.ChapterUpdate
import niaromi.domain.manga.interactor.UpdateManga
import niaromi.domain.manga.model.Manga
import niaromi.domain.manga.model.MangaUpdate
import niaromi.domain.manga.model.hasCustomCover
import niaromi.domain.migration.models.MigrationFlag
import niaromi.domain.source.interactor.UpdateMangaFromRemote
import niaromi.domain.source.service.SourceManager
import niaromi.domain.source.service.SourcePreferences
import niaromi.domain.track.interactor.GetTracks
import niaromi.domain.track.interactor.UpsertTrack
import kotlin.time.Clock

@Inject
class MigrateMangaUseCase(
    @AppCoroutineScope private val scope: CoroutineScope,
    private val sourcePreferences: SourcePreferences,
    private val trackerManager: TrackerManager,
    private val sourceManager: SourceManager,
    private val downloadManager: DownloadManager,
    private val updateManga: UpdateManga,
    private val getChaptersByMangaId: GetChaptersByMangaId,
    private val updateChapter: UpdateChapter,
    private val getCategories: GetCategories,
    private val setMangaCategories: SetMangaCategories,
    private val getTracks: GetTracks,
    private val upsertTrack: UpsertTrack,
    private val coverCache: CoverCache,
    private val updateMangaFromRemote: UpdateMangaFromRemote,
) {
    private val enhancedServices by lazy { trackerManager.trackers.filterIsInstance<EnhancedTracker>() }

    suspend operator fun invoke(current: Manga, target: Manga, replace: Boolean) {
        val targetSource = sourceManager.get(target.source) ?: return
        val currentSource = sourceManager.get(current.source)
        val flags = sourcePreferences.migrationFlags.get()

        try {
            updateMangaFromRemote(target, fetchChapters = true).getOrThrow()

            // Update chapters read, bookmark and dateFetch
            if (MigrationFlag.CHAPTER in flags) {
                val prevMangaChapters = getChaptersByMangaId.await(current.id)
                val mangaChapters = getChaptersByMangaId.await(target.id)

                val maxChapterRead = prevMangaChapters
                    .filter { it.read }
                    .maxOfOrNull { it.chapterNumber }

                val chapterUpdates = mangaChapters
                    .filter { it.isRecognizedNumber }
                    .map { mangaChapter ->
                        val prevChapter = prevMangaChapters
                            .find { it.isRecognizedNumber && it.chapterNumber == mangaChapter.chapterNumber }

                        ChapterUpdate(mangaChapter.id) {
                            if (prevChapter != null) {
                                dateFetch = prevChapter.dateFetch
                                bookmark = prevChapter.bookmark
                            }

                            if (maxChapterRead != null && mangaChapter.chapterNumber <= maxChapterRead) {
                                read = true
                            }
                        }
                    }
                updateChapter.awaitAll(chapterUpdates)
            }

            // Update categories
            if (MigrationFlag.CATEGORY in flags) {
                val categoryIds = getCategories.await(current.id).map { it.id }
                setMangaCategories.await(target.id, categoryIds)
            }

            // Update track
            getTracks.await(current.id).mapNotNull { track ->
                val updatedTrack = track.copy(mangaId = target.id)

                val service = enhancedServices
                    .firstOrNull { it.isTrackFrom(updatedTrack, current, currentSource) }

                if (service != null) {
                    service.migrateTrack(updatedTrack, target, targetSource)
                } else {
                    updatedTrack
                }
            }
                .takeIf { it.isNotEmpty() }
                ?.let { upsertTrack.awaitAll(it) }

            // Delete downloaded
            if (MigrationFlag.REMOVE_DOWNLOAD in flags && currentSource != null) {
                // Not awaited, so a slow storage delete doesn't hold up the migration
                scope.launch { downloadManager.deleteManga(current, currentSource) }
            }

            // Update custom cover (recheck if custom cover exists)
            if (MigrationFlag.CUSTOM_COVER in flags && current.hasCustomCover(coverCache)) {
                coverCache.getCustomCoverFile(current.id).inputStream().use {
                    coverCache.setCustomCoverToCache(target, it)
                }
            }

            val currentMangaUpdate = MangaUpdate(current.id) {
                favoriteAt = null
            }
                .takeIf { replace }
            val targetMangaUpdate = MangaUpdate(target.id) {
                favoriteAt = current.favoriteAt?.takeIf { replace } ?: Clock.System.now()
                chapterFlags = current.chapterFlags
                viewerFlags = current.viewerFlags
                if (MigrationFlag.NOTES in flags) notes = current.notes
            }

            updateManga.awaitAll(listOfNotNull(currentMangaUpdate, targetMangaUpdate))
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
        }
    }
}
