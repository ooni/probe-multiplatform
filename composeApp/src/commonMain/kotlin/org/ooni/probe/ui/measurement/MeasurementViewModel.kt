package org.ooni.probe.ui.measurement

import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import org.intellij.markdown.html.urlEncode
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementWithUrl

class MeasurementViewModel(
    measurementId: MeasurementModel.Id,
    onBack: () -> Unit,
    getMeasurement: (MeasurementModel.Id) -> Flow<MeasurementWithUrl?>,
    openUrl: (String) -> Unit,
    shareUrl: (String) -> Unit,
    isWebViewAvailable: () -> Boolean,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow<State>(State.CheckingWebViewAvailability)
    val state = _state.asStateFlow()

    init {
        getMeasurement(measurementId)
            .take(1)
            .onEach { item ->
                val m = item?.measurement
                val input = item?.url?.url
                val url = if (m?.uid != null) {
                    "${OrganizationConfig.explorerUrl}/m/${m.uid.value}?webview=true&language=${Locale.current.toLanguageTag()}"
                } else if (m?.reportId != null) {
                    val inputSuffix = input?.let { "?input=${urlEncode(it)}" } ?: ""
                    val separator = if (inputSuffix.isEmpty()) "?" else "&"
                    "${OrganizationConfig.explorerUrl}/measurement/${m.reportId.value}$inputSuffix${separator}webview=true&language=${Locale.current.toLanguageTag()}"
                } else {
                    onBack()
                    return@onEach
                }

                if (isWebViewAvailable()) {
                    _state.value = State.ShowMeasurement(url)
                } else {
                    openUrl(url)
                    onBack()
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ShareUrl>()
            .onEach { (_state.value as? State.ShowMeasurement)?.url?.let(shareUrl) }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    sealed interface State {
        data object CheckingWebViewAvailability : State

        data class ShowMeasurement(
            val url: String,
        ) : State
    }

    sealed interface Event {
        data object BackClicked : Event

        data object ShareUrl : Event
    }
}
