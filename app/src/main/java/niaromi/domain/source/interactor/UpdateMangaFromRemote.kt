package niaromi.domain.source.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import niaromi.core.common.util.system.logcat
import niaromi.data.cache.CoverCache
import niaromi.data.download.DownloadManager
import niaromi.domain.chapter.interactor.SyncChaptersWithSource
import niaromi.domain.chapter.model.Chapter
import niaromi.domain.chapter.model.toSChapter
import niaromi.domain.chapter.repository.ChapterRepository
import niaromi.domain.library.service.LibraryPreferences
import niaromi.domain.manga.model.Manga
import niaromi.domain.manga.model.MangaRemoteUpdate
import niaromi.domain.manga.model.hasCustomCover
import niaromi.domain.manga.model.toSManga
import niaromi.domain.manga.repository.MangaRepository
import niaromi.domain.source.models.RemoteMangaUpdate
import niaromi.domain.source.service.SourceManager
import niaromi.source.local.isLocal
import kotlin.time.Clock
import kotlin.time.Instant

@Inject
class UpdateMangaFromRemote(
    private val sourceManager: SourceManager,
    private val chapterRepository: ChapterRepository,
    private val mangaRepository: MangaRepository,
    private val syncChaptersWithSource: SyncChaptersWithSource,
    private val coverCache: CoverCache,
    private val libraryPreferences: LibraryPreferences,
    private val downloadManager: DownloadManager,
) {
    suspend operator fun invoke(
        manga: Manga,
        fetchDetails: Boolean = false,
        fetchChapters: Boolean = false,
        manualFetch: Boolean = false,
        fetchWindow: ClosedRange<Instant>? = null,
    ): Result<RemoteMangaUpdate> {
        val source = sourceManager.getOrStub(manga.source)
        return invoke(
            source = source,
            manga = manga,
            fetchDetails = fetchDetails,
            fetchChapters = fetchChapters,
            manualFetch = manualFetch,
        )
    }

    suspend operator fun invoke(
        source: Source,
        manga: Manga,
        fetchDetails: Boolean = false,
        fetchChapters: Boolean = false,
        manualFetch: Boolean = false,
        fetchWindow: ClosedRange<Instant>? = null,
        refreshCover: Boolean = false,
    ): Result<RemoteMangaUpdate> = withContext(Dispatchers.IO) {
        try {
            val chapters = chapterRepository.getChapterByMangaId(manga.id)
                .sortedBy { it.sourceOrder }
            val update = source.getMangaUpdate(
                manga = manga.toSManga(),
                chapters = chapters.map(Chapter::toSChapter),
                fetchDetails = fetchDetails,
                fetchChapters = fetchChapters,
            )
            awaitUpdateFromSource(manga, update.manga, manualFetch, refreshCover)
            val newChapters = syncChaptersWithSource.await(
                rawSourceChapters = update.chapters,
                manga = manga,
                source = source,
                manualFetch = manualFetch,
                fetchWindow = fetchWindow,
            )
            val updatedManga = mangaRepository.getMangaById(manga.id)

            Result.success(RemoteMangaUpdate(manga = updatedManga, newChapters = newChapters))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.failure(e)
        }
    }

    private suspend fun awaitUpdateFromSource(
        localManga: Manga,
        remoteManga: SManga,
        manualFetch: Boolean,
        refreshCover: Boolean,
    ): Boolean {
        val remoteTitle = try {
            remoteManga.title
        } catch (_: UninitializedPropertyAccessException) {
            ""
        }

        // if the manga isn't a favorite (or 'update titles' preference is enabled), set its title from source and update in db
        val title =
            if (remoteTitle.isNotEmpty() && (!localManga.favorite || libraryPreferences.updateMangaTitles.get())) {
                remoteTitle
            } else {
                null
            }

        val keepLibraryCover = localManga.favorite && !refreshCover &&
            coverCache.hasCachedCover(localManga.thumbnailUrl)
        val thumbnailUrl = remoteManga.thumbnail_url?.takeIf { it.isNotEmpty() && !keepLibraryCover }

        val coverLastModified = when {
            thumbnailUrl == null -> null
            !refreshCover && !manualFetch && localManga.thumbnailUrl == thumbnailUrl -> null
            localManga.isLocal() -> Clock.System.now()
            else -> {
                if (refreshCover) coverCache.deleteFromCache(localManga)
                Clock.System.now().takeUnless { localManga.hasCustomCover(coverCache) }
            }
        }

        val success = mangaRepository.updateRemote(
            MangaRemoteUpdate(
                id = localManga.id,
                title = title,
                coverLastModified = coverLastModified,
                author = remoteManga.author,
                artist = remoteManga.artist,
                description = remoteManga.description,
                genre = remoteManga.getGenres(),
                thumbnailUrl = thumbnailUrl,
                status = remoteManga.status.toLong(),
                updateStrategy = remoteManga.update_strategy,
                initialized = true,
                memo = remoteManga.memo,
            ),
        )
        if (success && title != null) {
            downloadManager.renameManga(localManga, title)
        }
        return success
    }
}
