package niaromi.data.track.mangaupdates.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import niaromi.data.database.models.Track
import niaromi.data.track.mangaupdates.MangaUpdates.Companion.READING_LIST

@Serializable
data class MUListItem(
    val series: MUSeries? = null,
    @SerialName("list_id")
    val listId: Long? = null,
    val status: MUStatus? = null,
    val priority: Int? = null,
) {
    fun copyTo(track: Track): Track {
        return track.apply {
            this.status = listId ?: READING_LIST
            this.last_chapter_read = this@MUListItem.status?.chapter?.toDouble() ?: 0.0
        }
    }
}
