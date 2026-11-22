package niaromi.domain.manga.interactor

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import logcat.LogPriority
import niaromi.core.common.util.system.logcat
import niaromi.domain.library.model.LibraryManga
import niaromi.domain.manga.repository.MangaRepository
import kotlin.time.Duration.Companion.seconds

@Inject
@SingleIn(AppScope::class)
class GetLibraryManga(
    private val mangaRepository: MangaRepository,
) {

    @Volatile
    var cached: List<LibraryManga>? = null
        private set

    suspend fun await(): List<LibraryManga> {
        return mangaRepository.getLibraryManga()
    }

    fun subscribe(): Flow<List<LibraryManga>> {
        return mangaRepository.getLibraryMangaAsFlow()
            .onEach { cached = it }
            .retry {
                if (it is NullPointerException) {
                    delay(0.5.seconds)
                    true
                } else {
                    false
                }
            }.catch {
                this@GetLibraryManga.logcat(LogPriority.ERROR, it)
            }
    }
}
