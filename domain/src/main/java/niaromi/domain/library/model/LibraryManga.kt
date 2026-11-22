package niaromi.domain.library.model

import niaromi.domain.manga.model.Manga
import kotlin.time.Instant

data class LibraryManga(
    val manga: Manga,
    val categories: List<Long>,
    val totalChapters: Long,
    val readCount: Long,
    val bookmarkCount: Long,
    val latestUpload: Instant?,
    val chapterFetchedAt: Instant?,
    val lastRead: Instant?,
) {
    val id: Long = manga.id

    val unreadCount
        get() = totalChapters - readCount

    val hasBookmarks
        get() = bookmarkCount > 0

    val hasStarted = readCount > 0

    val hasLatestChapters: Boolean
        get() {
            if (unreadCount <= 0L) return false
            val favAt = manga.favoriteAt ?: return false

            if (lastRead != null) {
                return chapterFetchedAt != null && chapterFetchedAt > lastRead
            }

            if (favAt > niaromi.domain.manga.model.Manga.UNKNOWN_FAVORITE_AT &&
                chapterFetchedAt != null &&
                chapterFetchedAt > favAt
            ) {
                return true
            }

            return false
        }
}
