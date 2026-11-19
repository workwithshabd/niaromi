package niaromi.util

import niaromi.data.cache.CoverCache
import niaromi.domain.manga.interactor.UpdateManga
import niaromi.domain.manga.model.Manga
import niaromi.domain.manga.model.toSManga
import niaromi.source.local.image.LocalCoverManager
import niaromi.source.local.isLocal
import java.io.InputStream

fun Manga.removeCovers(coverCache: CoverCache) {
    if (isLocal()) return
    coverCache.deleteFromCache(this, true)
}

suspend fun Manga.editCover(
    coverManager: LocalCoverManager,
    stream: InputStream,
    updateManga: UpdateManga,
    coverCache: CoverCache,
) {
    if (isLocal()) {
        coverManager.update(toSManga(), stream)
        updateManga.awaitUpdateCoverLastModified(id)
    } else if (favorite) {
        coverCache.setCustomCoverToCache(this, stream)
        updateManga.awaitUpdateCoverLastModified(id)
    }
}
