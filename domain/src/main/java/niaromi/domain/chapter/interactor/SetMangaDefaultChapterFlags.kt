package niaromi.domain.chapter.interactor

import dev.zacsweers.metro.Inject
import niaromi.domain.library.service.LibraryPreferences
import niaromi.domain.manga.interactor.GetFavorites
import niaromi.domain.manga.interactor.SetMangaChapterFlags
import niaromi.domain.manga.model.Manga

@Inject
class SetMangaDefaultChapterFlags(
    private val libraryPreferences: LibraryPreferences,
    private val setMangaChapterFlags: SetMangaChapterFlags,
    private val getFavorites: GetFavorites,
) {

    suspend fun await(manga: Manga) {
        setDefaultFlags(listOf(manga.id))
    }

    suspend fun awaitAll() {
        setDefaultFlags(getFavorites.await().map { it.id })
    }

    private suspend fun setDefaultFlags(mangaIds: List<Long>) {
        with(libraryPreferences) {
            setMangaChapterFlags.awaitSetAllFlags(
                mangaIds = mangaIds,
                unreadFilter = filterChapterByRead.get(),
                downloadedFilter = filterChapterByDownloaded.get(),
                bookmarkedFilter = filterChapterByBookmarked.get(),
                sortingMode = sortChapterBySourceOrNumber.get(),
                sortingDirection = sortChapterByAscendingOrDescending.get(),
                displayMode = displayChapterByNameOrNumber.get(),
            )
        }
    }
}
