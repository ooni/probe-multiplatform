package org.ooni.probe.ui.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import okio.Path
import ooniprobe.composeapp.generated.resources.Measurement_Raw_Share
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.getString
import org.ooni.probe.data.disk.ReadFile
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementWithUrl
import org.ooni.probe.data.models.PlatformAction

class MeasurementRawViewModel(
    measurementId: MeasurementModel.Id,
    onBack: () -> Unit,
    goToUpload: (MeasurementModel.Id) -> Unit,
    goToMeasurement: (MeasurementModel.Id) -> Unit,
    getMeasurement: (MeasurementModel.Id) -> Flow<MeasurementWithUrl?>,
    readFile: ReadFile,
    shareFile: (PlatformAction.FileSharing) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        getMeasurement(measurementId)
            .filter { it == null }
            .take(1)
            .onEach { onBack() }
            .launchIn(viewModelScope)

        getMeasurement(measurementId)
            .filterNotNull()
            .filter { it.measurement.isDone && !it.measurement.isMissingUpload }
            .take(1)
            .onEach { it.measurement.id?.let(goToMeasurement) }
            .launchIn(viewModelScope)

        getMeasurement(measurementId)
            .filterNotNull()
            .take(1)
            .onEach { item ->
                item.measurement.reportFilePath?.let { reportFilePath ->
                    val json = readFile(reportFilePath)
                    val jsonPretty = json?.let {
                        val jsonSerializer = Json { prettyPrint = true }
                        jsonSerializer.encodeToString(jsonSerializer.parseToJsonElement(it))
                    }
                    _state.update {
                        it.copy(
                            reportFilePath = reportFilePath,
                            json = jsonPretty,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.UploadClicked>()
            .onEach { goToUpload(measurementId) }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.ShareClicked>()
            .onEach {
                _state.value.reportFilePath?.let {
                    shareFile(
                        PlatformAction.FileSharing(getString(Res.string.Measurement_Raw_Share), it),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val json: String? = null,
        val reportFilePath: Path? = null,
    )

    sealed interface Event {
        data object BackClicked : Event

        data object UploadClicked : Event

        data object ShareClicked : Event
    }
}
