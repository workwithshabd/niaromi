package niaromi.domain.history.interactor

import dev.zacsweers.metro.Inject
import niaromi.domain.history.model.HistoryUpdate
import niaromi.domain.history.repository.HistoryRepository

@Inject
class UpsertHistory(
    private val historyRepository: HistoryRepository,
) {

    suspend fun await(historyUpdate: HistoryUpdate) {
        historyRepository.upsertHistory(historyUpdate)
    }
}
