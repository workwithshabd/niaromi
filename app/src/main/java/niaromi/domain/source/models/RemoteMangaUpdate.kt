package niaromi.domain.source.models

import niaromi.domain.chapter.model.Chapter
import niaromi.domain.manga.model.Manga

data class RemoteMangaUpdate(
    val manga: Manga,
    val newChapters: List<Chapter>,
)
