package org.ooni.probe.ui.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MeasurementViewModel(
    onBack: () -> Unit,
    shareUrl: (String) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    init {
        events.filterIsInstance<Event.BackClicked>().onEach { onBack() }.launchIn(viewModelScope)
        events.filterIsInstance<Event.ShareUrl>().onEach { url -> shareUrl(url.url) }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    sealed interface Event {
        data object BackClicked : Event

        data class ShareUrl(val url: String) : Event
    }
}
