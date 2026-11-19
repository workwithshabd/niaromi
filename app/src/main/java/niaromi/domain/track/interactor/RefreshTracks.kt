package niaromi.domain.track.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import niaromi.data.track.Tracker
import niaromi.data.track.TrackerManager
import niaromi.domain.track.interactor.GetTracks
import niaromi.domain.track.interactor.UpsertTrack
import niaromi.domain.track.model.toDbTrack
import niaromi.domain.track.model.toDomainTrack

@Inject
class RefreshTracks(
    private val getTracks: GetTracks,
    private val trackerManager: TrackerManager,
    private val upsertTrack: UpsertTrack,
    private val syncChapterProgressWithTrack: SyncChapterProgressWithTrack,
) {

    /**
     * Fetches updated tracking data from all logged in trackers.
     *
     * @return Failed updates.
     */
    suspend fun await(mangaId: Long): List<Pair<Tracker?, Throwable>> {
        return supervisorScope {
            return@supervisorScope getTracks.await(mangaId)
                .map { it to trackerManager.get(it.trackerId) }
                .filter { (_, service) -> service?.isLoggedIn == true }
                .map { (track, service) ->
                    async(Dispatchers.IO) {
                        return@async try {
                            val updatedTrack = service!!.refresh(track.toDbTrack()).toDomainTrack()!!
                            upsertTrack.await(updatedTrack)
                            syncChapterProgressWithTrack.await(mangaId, updatedTrack, service)
                            null
                        } catch (e: Throwable) {
                            service to e
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
    }
}
