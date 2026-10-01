package org.ooni.probe.ui.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementWithUrl

class MeasurementViewModel(
    measurementId: MeasurementModel.Id,
    onBack: () -> Unit,
    getMeasurement: (MeasurementModel.Id) -> Flow<MeasurementWithUrl?>,
    openUrl: (String) -> Unit,
    shareUrl: (String) -> Boolean,
    isWebViewAvailable: () -> Boolean,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow<State>(State.CheckingWebViewAvailability)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val measurement = getMeasurement(measurementId).first()
            val webViewUrl = measurement?.webViewUrl
            val explorerUrl = measurement?.explorerUrl
            if (webViewUrl == null || explorerUrl == null) {
                onBack()
                return@launch
            }

            if (isWebViewAvailable()) {
                _state.value = State.ShowMeasurement(webViewUrl = webViewUrl, explorerUrl = explorerUrl)
            } else {
                openUrl(webViewUrl)
                onBack()
            }
        }

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ShareUrl>()
            .onEach {
                val state = _state.value as? State.ShowMeasurement ?: return@onEach
                if (!shareUrl(state.explorerUrl)) {
                    _state.value = state.copy(copyMessageToClipboard = state.explorerUrl)
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.MessageCopied>()
            .onEach {
                val state = _state.value as? State.ShowMeasurement ?: return@onEach
                _state.value = state.copy(copyMessageToClipboard = null)
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    sealed interface State {
        data object CheckingWebViewAvailability : State

        data class ShowMeasurement(
            val webViewUrl: String,
            val explorerUrl: String,
            val copyMessageToClipboard: String? = null,
        ) : State
    }

    sealed interface Event {
        data object BackClicked : Event

        data object ShareUrl : Event

        data object MessageCopied : Event
    }
}
