package org.ooni.probe.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.ResultItem
import org.ooni.probe.data.models.ResultModel

class ResultViewModel(
    resultId: ResultModel.Id,
    onBack: () -> Unit,
    goToMeasurement: (MeasurementModel.ReportId, String?) -> Unit,
    goToUpload: () -> Unit,
    getResult: (ResultModel.Id) -> Flow<ResultItem?>,
    markResultAsViewed: suspend (ResultModel.Id) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State(result = null))
    val state = _state.asStateFlow()

    init {
        getResult(resultId)
            .onEach { result ->
                _state.update { it.copy(result = result) }
                if (result?.result?.isViewed == false) {
                    markResultAsViewed(resultId)
                }
            }
            .launchIn(viewModelScope)

        _state
            .map { it.result }
            .filterNotNull()
            .take(1)
            .onEach {
                if (!it.result.isViewed) {
                    markResultAsViewed(resultId)
                }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.MeasurementClicked>()
            .onEach { goToMeasurement(it.measurementReportId, it.measurementInput) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UploadClicked>()
            .onEach { goToUpload() }
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

        data object UploadClicked : Event
    }
}
