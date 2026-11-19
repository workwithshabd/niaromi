package eu.kanade.tachiyomi.data.backup.google.models

import kotlinx.serialization.Serializable

@Serializable
data class GoogleDriveFile(
    val id: String,
    val name: String,
    val size: Long? = null,
    val modifiedTime: String? = null,
)

@Serializable
data class GoogleDriveFileList(
    val files: List<GoogleDriveFile> = emptyList(),
)
