package niaromi.data.track.mangaupdates.dto

import kotlinx.serialization.Serializable
import niaromi.data.database.models.Track

@Serializable
data class MURating(
    val rating: Double? = null,
) {
    fun copyTo(track: Track): Track {
        return track.apply {
            this.score = rating ?: 0.0
        }
    }
}
