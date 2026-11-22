package niaromi.domain.migration.usecases

import io.kotest.matchers.shouldBe
import niaromi.domain.chapter.model.Chapter
import org.junit.jupiter.api.Test

class MigrateChapterProgressTest {
    @Test
    fun `migration carries read chapters without filling gaps or duplicating progress`() {
        val previous = listOf(
            Chapter.create().copy(chapterNumber = 1.0, read = true),
            Chapter.create().copy(chapterNumber = 2.0),
            Chapter.create().copy(chapterNumber = 3.0, read = true),
            Chapter.create().copy(name = "Bonus Story", read = true),
        )
        val target = listOf(
            Chapter.create().copy(id = 1, chapterNumber = 1.0),
            Chapter.create().copy(id = 2, chapterNumber = 2.0),
            Chapter.create().copy(id = 3, chapterNumber = 3.0),
            Chapter.create().copy(id = 4, chapterNumber = 3.0),
            Chapter.create().copy(id = 5, name = "bonus story"),
        )

        migrateChapterProgress(previous, target).filter { it.read == true }.map { it.id } shouldBe listOf(1L, 3L, 5L)
    }

    @Test
    fun `existing target progress does not cause duplicate variants to be marked read`() {
        val previous = listOf(Chapter.create().copy(chapterNumber = 1.0, read = true))
        val target = listOf(
            Chapter.create().copy(id = 1, chapterNumber = 1.0),
            Chapter.create().copy(id = 2, chapterNumber = 1.0, read = true),
        )

        migrateChapterProgress(previous, target).any { it.read == true } shouldBe false
    }
}
