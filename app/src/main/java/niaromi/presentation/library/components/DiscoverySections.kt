package niaromi.presentation.library.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import niaromi.domain.category.model.Category
import niaromi.domain.library.model.DiscoverySort
import niaromi.domain.library.model.LibraryManga
import niaromi.presentation.components.DropdownMenu

const val LATEST_CHAPTERS_SECTION_ID = -1L
const val NO_GENRE_SECTION_ID = -2L

data class DiscoveryCategorySection(
    val id: Long,
    val name: String,
    val items: List<LibraryManga>,
) {
    fun matches(sectionId: Long?, sectionName: String?): Boolean {
        return if (id < NO_GENRE_SECTION_ID) {
            (sectionId == null || sectionId < NO_GENRE_SECTION_ID) && name.equals(sectionName, ignoreCase = true)
        } else {
            id == sectionId
        }
    }
}

fun buildDiscoverySections(
    categories: List<Category>,
    favorites: List<LibraryManga>,
    sort: DiscoverySort,
    getItemsForCategory: (Category) -> List<LibraryManga> = { category ->
        favorites.filter { category.id in it.categories }
    },
): List<DiscoveryCategorySection> {
    val sections = buildList {
        val updated = favorites.filter { it.hasLatestChapters }
        if (updated.isNotEmpty()) {
            add(DiscoveryCategorySection(LATEST_CHAPTERS_SECTION_ID, "Latest Chapters", updated))
        }
        categories.filterNot { it.isSystemCategory }.forEach { category ->
            val items = getItemsForCategory(category)
            if (items.isNotEmpty()) add(DiscoveryCategorySection(category.id, category.name, items))
        }
        val byGenre = favorites.groupBy { it.manga.primaryGenre?.trim()?.takeIf(String::isNotEmpty)?.lowercase() }
        byGenre.entries.filter { it.key != null }
            .sortedWith(compareByDescending<Map.Entry<String?, List<LibraryManga>>> { it.value.size }.thenBy { it.key })
            .forEachIndexed { index, (_, items) ->
                val name = items.first().manga.primaryGenre.orEmpty().trim().replaceFirstChar { it.titlecase() }
                add(DiscoveryCategorySection(NO_GENRE_SECTION_ID - 1 - index, name, items))
            }
        byGenre[null]?.let {
            add(DiscoveryCategorySection(NO_GENRE_SECTION_ID, if (isEmpty()) "All Titles" else "More Favourites", it))
        }
    }
    val sorted = when (sort) {
        DiscoverySort.Default, DiscoverySort.LastRead -> sections
        DiscoverySort.Title -> sections.sortedBy { it.name.lowercase() }
        DiscoverySort.Unread -> sections.sortedWith(
            compareByDescending<DiscoveryCategorySection> { section -> section.items.sumOf { it.unreadCount } }
                .thenBy { it.name.lowercase() },
        )
    }
    return sorted.map { section ->
        val sectionSort = if (section.id == LATEST_CHAPTERS_SECTION_ID && sort == DiscoverySort.LastRead) {
            DiscoverySort.Default
        } else {
            sort
        }
        section.copy(items = sortDiscoveryTitles(section.items, sectionSort, section.id))
    }
}

fun sortDiscoveryTitles(items: List<LibraryManga>, sort: DiscoverySort, sectionId: Long): List<LibraryManga> {
    val byTitle = compareBy<LibraryManga> { it.manga.title.lowercase() }.thenBy { it.id }
    val comparator = when (sort) {
        DiscoverySort.Title -> byTitle
        DiscoverySort.LastRead -> compareByDescending<LibraryManga> { it.lastRead }.then(byTitle)
        DiscoverySort.Unread -> compareByDescending<LibraryManga> { it.unreadCount }.then(byTitle)
        DiscoverySort.Default -> {
            if (sectionId == LATEST_CHAPTERS_SECTION_ID) {
                compareByDescending<LibraryManga> { it.chapterFetchedAt }
                    .thenByDescending { it.manga.lastUpdate }
                    .thenByDescending { it.latestUpload }
            } else {
                compareByDescending<LibraryManga> { it.lastRead }
            }.thenByDescending { it.manga.favoriteAt }.then(byTitle)
        }
    }
    return items.sortedWith(comparator)
}

@Composable
fun DiscoverySortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSort: (DiscoverySort) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DiscoverySort.entries.forEach { sort ->
            DropdownMenuItem(
                text = { Text(sort.label) },
                onClick = {
                    onSort(sort)
                    onDismiss()
                },
            )
        }
    }
}
