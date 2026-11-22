package niaromi.domain.backup.repository

import niaromi.domain.backup.model.RestoredManga
import niaromi.domain.manga.model.Manga
import niaromi.domain.manga.model.MangaUpdate

interface RestoreRepository {

    /**
     * Restores [entries] all together: either every one of them is restored or none is. [update] is what to change on each
     * entry once its chapters and history are in place, and is part of the same all-or-nothing restore.
     */
    suspend fun restoreManga(entries: List<RestoredManga>, update: suspend (Manga) -> MangaUpdate)
}
