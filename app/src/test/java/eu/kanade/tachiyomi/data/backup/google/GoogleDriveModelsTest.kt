package eu.kanade.tachiyomi.data.backup.google

import eu.kanade.tachiyomi.data.backup.google.models.GoogleAccount
import eu.kanade.tachiyomi.data.backup.google.models.GoogleDriveFile
import eu.kanade.tachiyomi.data.backup.google.models.GoogleDriveFileList
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class GoogleDriveModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `serialize and deserialize GoogleDriveFile`() {
        val file = GoogleDriveFile(
            id = "drive-file-id-123",
            name = "app.niaromi_2026-11-19_13-00.tachibk",
            size = 1048576L,
            modifiedTime = "2026-11-19T07:30:00.000Z",
        )

        val encoded = json.encodeToString(GoogleDriveFile.serializer(), file)
        val decoded = json.decodeFromString(GoogleDriveFile.serializer(), encoded)

        decoded shouldBe file
    }

    @Test
    fun `deserialize GoogleDriveFileList with various fields`() {
        val apiResponse = """
            {
              "kind": "drive#fileList",
              "incompleteSearch": false,
              "files": [
                {
                  "kind": "drive#file",
                  "id": "file-1",
                  "name": "app.niaromi_2026-11-19_12-00.tachibk",
                  "mimeType": "application/octet-stream",
                  "size": "2048",
                  "modifiedTime": "2026-11-19T06:30:00.000Z"
                },
                {
                  "kind": "drive#file",
                  "id": "file-2",
                  "name": "app.niaromi_2026-11-18_10-00.tachibk",
                  "mimeType": "application/octet-stream"
                }
              ]
            }
        """.trimIndent()

        val result = json.decodeFromString(GoogleDriveFileList.serializer(), apiResponse)
        result.files shouldHaveSize 2
        result.files[0].id shouldBe "file-1"
        result.files[0].name shouldBe "app.niaromi_2026-11-19_12-00.tachibk"
        result.files[0].size shouldBe 2048L
        result.files[0].modifiedTime shouldBe "2026-11-19T06:30:00.000Z"

        result.files[1].id shouldBe "file-2"
        result.files[1].name shouldBe "app.niaromi_2026-11-18_10-00.tachibk"
        result.files[1].size shouldBe null
    }

    @Test
    fun `GoogleAccount properties`() {
        val account = GoogleAccount(
            email = "reader@example.com",
            displayName = "Manga Reader",
            photoUrl = "https://example.com/avatar.png",
        )

        account.email shouldBe "reader@example.com"
        account.displayName shouldBe "Manga Reader"
        account.photoUrl shouldBe "https://example.com/avatar.png"
    }

    @Test
    fun `backup data structure holds bookmarks, chapter read, and title details`() {
        val manga = BackupManga(
            source = 1L,
            url = "/manga/sample-title",
            title = "Sample Manga Title",
            artist = "Sample Artist",
            author = "Sample Author",
            description = "A great manga about adventure.",
            genre = listOf("Action", "Adventure"),
            chapters = listOf(
                BackupChapter(
                    url = "/chapter/1",
                    name = "Chapter 1",
                    read = true,
                    bookmark = true,
                    lastPageRead = 24,
                    chapterNumber = 1f,
                ),
                BackupChapter(
                    url = "/chapter/2",
                    name = "Chapter 2",
                    read = false,
                    bookmark = false,
                    lastPageRead = 0,
                    chapterNumber = 2f,
                ),
            ),
        )

        val backup = Backup(
            backupManga = listOf(manga),
        )

        backup.backupManga shouldHaveSize 1
        val item = backup.backupManga.first()
        item.title shouldBe "Sample Manga Title"
        item.author shouldBe "Sample Author"
        item.chapters shouldHaveSize 2

        val chapter1 = item.chapters[0]
        chapter1.read shouldBe true
        chapter1.bookmark shouldBe true
        chapter1.lastPageRead shouldBe 24

        val chapter2 = item.chapters[1]
        chapter2.read shouldBe false
        chapter2.bookmark shouldBe false
    }
}
