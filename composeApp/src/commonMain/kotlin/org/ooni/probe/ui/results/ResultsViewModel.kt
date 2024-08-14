package org.ooni.probe.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.data.models.ResultModel

class ResultsViewModel(
    goToResult: (ResultModel.Id) -> Unit,
    getResults: () -> Flow<List<ResultListItem>>,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state =
        MutableStateFlow(
            State(
                results = emptyMap(),
                isLoading = true,
            ),
        )
    val state = _state.asStateFlow()

    init {
        getResults()
            .onEach { results ->
                val groupedResults = results.groupBy { it.monthAndYear }
                _state.update { it.copy(results = groupedResults) }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ResultClick>()
            .onEach { goToResult(it.result.idOrThrow) }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private val ResultListItem.monthAndYear
        get() =
            result.startTime.let { startTime ->
                LocalDate(year = startTime.year, month = startTime.month, dayOfMonth = 1)
            }

    data class State(
        val results: Map<LocalDate, List<ResultListItem>>,
        val isLoading: Boolean,
    )

    sealed interface Event {
        data class ResultClick(val result: ResultListItem) : Event
    }
}
