package niaromi.domain.track.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import niaromi.core.common.util.system.logcat
import niaromi.domain.track.model.Track
import niaromi.domain.track.repository.TrackRepository

@Inject
class GetTracks(
    private val trackRepository: TrackRepository,
) {

    suspend fun awaitOne(id: Long): Track? {
        return try {
            trackRepository.getTrackById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    suspend fun await(mangaId: Long): List<Track> {
        return try {
            trackRepository.getTracksByMangaId(mangaId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    fun subscribe(mangaId: Long): Flow<List<Track>> {
        return trackRepository.getTracksByMangaIdAsFlow(mangaId)
    }
}
