package niaromi.domain.track.interactor

import android.content.Context
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import logcat.LogPriority
import niaromi.core.common.util.system.logcat
import niaromi.data.track.TrackerManager
import niaromi.domain.track.interactor.GetTracks
import niaromi.domain.track.interactor.UpsertTrack
import niaromi.domain.track.model.toDbTrack
import niaromi.domain.track.model.toDomainTrack
import niaromi.domain.track.service.DelayedTrackingUpdateWorker
import niaromi.domain.track.store.DelayedTrackingStore

@Inject
class TrackChapter(
    private val getTracks: GetTracks,
    private val trackerManager: TrackerManager,
    private val upsertTrack: UpsertTrack,
    private val delayedTrackingStore: DelayedTrackingStore,
) {

    suspend fun await(context: Context, mangaId: Long, chapterNumber: Double, setupJobOnFailure: Boolean = true) {
        coroutineScope {
            val tracks = getTracks.await(mangaId)
            if (tracks.isEmpty()) return@coroutineScope

            tracks.mapNotNull { track ->
                val service = trackerManager.get(track.trackerId)
                if (service == null || !service.isLoggedIn || chapterNumber <= track.lastChapterRead) {
                    return@mapNotNull null
                }

                async {
                    runCatching {
                        try {
                            val updatedTrack = service.refresh(track.toDbTrack())
                                .toDomainTrack(idRequired = true)!!
                                .copy(lastChapterRead = chapterNumber)
                            service.update(updatedTrack.toDbTrack(), true)
                            upsertTrack.await(updatedTrack)
                            delayedTrackingStore.remove(track.id)
                        } catch (e: Exception) {
                            delayedTrackingStore.add(track.id, chapterNumber)
                            if (setupJobOnFailure) {
                                DelayedTrackingUpdateWorker.setupTask(context)
                            }
                            throw e
                        }
                    }
                }
            }
                .awaitAll()
                .mapNotNull { it.exceptionOrNull() }
                .forEach { logcat(LogPriority.WARN, it) }
        }
    }
}
