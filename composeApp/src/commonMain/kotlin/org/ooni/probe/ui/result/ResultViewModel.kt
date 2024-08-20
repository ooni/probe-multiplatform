package org.ooni.probe.ui.result

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
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.ResultItem
import org.ooni.probe.data.models.ResultModel

class ResultViewModel(
    resultId: ResultModel.Id,
    onBack: () -> Unit,
    goToMeasurement: (MeasurementModel.ReportId, String?) -> Unit,
    getResult: (ResultModel.Id) -> Flow<ResultItem?>,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State(result = null))
    val state = _state.asStateFlow()

    init {
        getResult(resultId)
            .onEach { result -> _state.update { it.copy(result = result) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.MeasurementClicked>()
            .onEach { goToMeasurement(it.measurementReportId, it.measurementInput) }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val result: ResultItem?,
    )

    sealed interface Event {
        data object BackClicked : Event

        data class MeasurementClicked(
            val measurementReportId: MeasurementModel.ReportId,
            val measurementInput: String?,
        ) : Event
    }
}
