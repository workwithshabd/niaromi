package niaromi.data.track

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.combine
import niaromi.data.track.anilist.Anilist
import niaromi.data.track.bangumi.Bangumi
import niaromi.data.track.comick.Comick
import niaromi.data.track.hikka.Hikka
import niaromi.data.track.kavita.Kavita
import niaromi.data.track.kitsu.Kitsu
import niaromi.data.track.komga.Komga
import niaromi.data.track.mangabaka.MangaBaka
import niaromi.data.track.mangaupdates.MangaUpdates
import niaromi.data.track.myanimelist.MyAnimeList
import niaromi.data.track.shikimori.Shikimori
import niaromi.data.track.suwayomi.Suwayomi

@Inject
@SingleIn(AppScope::class)
class TrackerManager(
    val myAnimeList: MyAnimeList,
    val aniList: Anilist,
    val kitsu: Kitsu,
    val shikimori: Shikimori,
    val bangumi: Bangumi,
    val komga: Komga,
    val mangaUpdates: MangaUpdates,
    val kavita: Kavita,
    val suwayomi: Suwayomi,
    val hikka: Hikka,
    val mangaBaka: MangaBaka,
    val comick: Comick,
) {

    val trackers = listOf(
        mangaBaka,
        myAnimeList,
        aniList,
        kitsu,
        shikimori,
        bangumi,
        komga,
        mangaUpdates,
        kavita,
        suwayomi,
        hikka,
        comick,
    )

    fun loggedInTrackers() = trackers.filter { it.isLoggedIn }

    fun loggedInTrackersFlow() = combine(trackers.map { it.isLoggedInFlow }) {
        it.mapIndexedNotNull { index, isLoggedIn ->
            if (isLoggedIn) trackers[index] else null
        }
    }

    fun get(id: Long) = trackers.find { it.id == id }

    fun getAll(ids: Set<Long>) = trackers.filter { it.id in ids }
}
