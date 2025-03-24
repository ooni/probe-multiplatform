package org.ooni.probe.ui.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.intellij.markdown.html.urlEncode
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.MeasurementModel

class MeasurementViewModel(
    reportId: MeasurementModel.ReportId,
    input: String?,
    onBack: () -> Unit,
    openUrl: (String) -> Unit,
    shareUrl: (String) -> Unit,
    isWebViewAvailable: () -> Boolean,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow<State>(State.CheckingWebViewAvailability)
    val state = _state.asStateFlow()

    init {
        val inputSuffix = input?.let { "?input=${urlEncode(it)}" } ?: ""
        val url = "${OrganizationConfig.explorerUrl}/measurement/${reportId.value}$inputSuffix"

        events.filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.ShareUrl>()
            .onEach { shareUrl(url) }
            .launchIn(viewModelScope)

        if (isWebViewAvailable()) {
            _state.value = State.ShowMeasurement(url)
        } else {
            openUrl(url)
            onBack()
        }
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    sealed interface State {
        data object CheckingWebViewAvailability : State

        data class ShowMeasurement(val url: String) : State
    }

    sealed interface Event {
        data object BackClicked : Event

        data object ShareUrl : Event
    }
}
