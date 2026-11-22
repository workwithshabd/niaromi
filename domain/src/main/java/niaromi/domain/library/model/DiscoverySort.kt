package niaromi.domain.library.model

enum class DiscoverySort(val label: String) {
    Default("Default order"),
    Title("Sort by title"),
    Unread("Sort by unread"),
    LastRead("Sort by last read"),
}
