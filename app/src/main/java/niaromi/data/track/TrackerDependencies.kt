package niaromi.data.track

import android.content.Context
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.network.NetworkHelper
import niaromi.domain.track.interactor.AddTracks
import niaromi.domain.track.interactor.UpsertTrack
import niaromi.domain.track.service.TrackPreferences

@Inject
class TrackerDependencies(
    val context: Context,
    val trackPreferences: TrackPreferences,
    val networkHelper: Lazy<NetworkHelper>,
    val addTracks: Lazy<AddTracks>,
    val upsertTrack: Lazy<UpsertTrack>,
)
