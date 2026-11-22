package niaromi.ui.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import logcat.LogPriority
import niaromi.core.common.util.system.logcat
import niaromi.core.util.insertSeparators
import niaromi.domain.history.interactor.GetHistory
import niaromi.domain.history.interactor.RemoveHistory
import niaromi.domain.history.model.HistoryWithRelations
import niaromi.presentation.history.HistoryUiModel
import niaromi.util.lang.toLocalDate

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class HistoryViewModel(
    private val getHistory: GetHistory,
    private val removeHistory: RemoveHistory,
) : ViewModel() {

    val snackbarHostState: SnackbarHostState = SnackbarHostState()

    private val searchQuery = MutableStateFlow<String?>(null)
    private val dialog = MutableStateFlow<Dialog?>(null)

    private val history = searchQuery
        .flatMapLatest { query ->
            getHistory.subscribe(query ?: "")
                .distinctUntilChanged()
                .catch { error ->
                    logcat(LogPriority.ERROR, error) { "Error fetching history" }
                }
                .map { it.toHistoryUiModels() }
                .flowOn(Dispatchers.Default)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val state: StateFlow<State> = combine(
        searchQuery,
        dialog,
        history,
    ) { searchQuery, dialog, history ->
        State(
            searchQuery = searchQuery,
            dialog = dialog,
            list = history,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State())

    fun search(query: String?) {
        searchQuery.update { query }
    }

    fun setDialog(dialog: Dialog?) {
        this.dialog.update { dialog }
    }

    fun delete(history: HistoryWithRelations) {
        viewModelScope.launch {
            removeHistory.await(history)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            removeHistory.awaitAll()
        }
    }

    private fun List<HistoryWithRelations>.toHistoryUiModels(): List<HistoryUiModel> {
        return map { HistoryUiModel.Item(it) }
            .insertSeparators { before, after ->
                val beforeDate = before?.item?.readAt?.toLocalDate()
                val afterDate = after?.item?.readAt?.toLocalDate()
                when {
                    beforeDate != afterDate && afterDate != null -> HistoryUiModel.Header(afterDate)
                    else -> null
                }
            }
    }

    sealed interface Dialog {
        data class Delete(val item: HistoryWithRelations) : Dialog
        data object DeleteAll : Dialog
    }

    @Immutable
    data class State(
        val searchQuery: String? = null,
        val dialog: Dialog? = null,
        val list: List<HistoryUiModel>? = null,
    )
}
