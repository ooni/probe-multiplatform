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
    goToUpload: () -> Unit,
    getResults: () -> Flow<List<ResultListItem>>,
    deleteAllResults: suspend () -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        getResults()
            .onEach { results ->
                val groupedResults = results.groupBy { it.monthAndYear }
                _state.update {
                    it.copy(
                        results = groupedResults,
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ResultClick>()
            .onEach { goToResult(it.result.idOrThrow) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UploadClick>()
            .onEach { goToUpload() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DeleteAllClick>()
            .onEach { deleteAllResults() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private val ResultListItem.monthAndYear
        get() = result.startTime.let { startTime ->
            LocalDate(year = startTime.year, month = startTime.month, dayOfMonth = 1)
        }

    data class State(
        val results: Map<LocalDate, List<ResultListItem>> = emptyMap(),
        val isLoading: Boolean = true,
    ) {
        val anyMissingUpload
            get() = results.any { it.value.any { item -> !item.allMeasurementsUploaded } }
    }

    sealed interface Event {
        data class ResultClick(val result: ResultListItem) : Event

        data object UploadClick : Event

        data object DeleteAllClick : Event
    }
}
