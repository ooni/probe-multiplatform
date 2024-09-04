package org.ooni.probe.ui.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import org.ooni.probe.domain.UploadMissingMeasurements

class UploadMeasurementsViewModel(
    onClose: () -> Unit,
    uploadMissingMeasurements: () -> Flow<UploadMissingMeasurements.State>,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state =
        MutableStateFlow<UploadMissingMeasurements.State>(UploadMissingMeasurements.State.Starting)
    val state = _state.asStateFlow()

    init {
        var uploadJob: Job? = null

        events
            .filterIsInstance<Event.RetryClick>()
            .onStart { emit(Event.RetryClick) } // Start to upload right away
            .onEach {
                uploadJob = uploadMissingMeasurements()
                    .onEach { _state.value = it }
                    .launchIn(viewModelScope)
            }
            .launchIn(viewModelScope)

        state
            .filter { it is UploadMissingMeasurements.State.Finished && it.failedToUpload == 0 }
            .take(1)
            .onEach { onClose() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.CancelClick>()
            .take(1)
            .onEach {
                uploadJob?.cancel()
                onClose()
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.CloseClick>()
            .take(1)
            .onEach { onClose() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    sealed interface Event {
        data object CancelClick : Event

        data object RetryClick : Event

        data object CloseClick : Event
    }
}
