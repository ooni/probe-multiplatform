package org.ooni.probe.ui.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.data.models.PlatformAction

class ArticleViewModel(
    url: ArticleModel.Url,
    onBack: () -> Unit,
    launchAction: (PlatformAction) -> Boolean,
    isWebViewAvailable: () -> Boolean,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow<State>(State.CheckingWebViewAvailability)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (isWebViewAvailable()) {
                _state.value = State.Show(url.value + "?enable-embedded-view=true")
            } else {
                launchAction(PlatformAction.OpenUrl(url.value))
                onBack()
            }
        }

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.OpenExternal>()
            .onEach { launchAction(PlatformAction.OpenUrl(url.value)) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ShareUrl>()
            .onEach {
                val state = _state.value as? State.Show ?: return@onEach
                if (!launchAction(PlatformAction.Share(url.value))) {
                    _state.value = state.copy(copyMessageToClipboard = url.value)
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.MessageCopied>()
            .onEach {
                val state = _state.value as? State.Show ?: return@onEach
                _state.value = state.copy(copyMessageToClipboard = null)
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.OutsideLinkClicked>()
            .onEach { launchAction(PlatformAction.OpenUrl(it.url)) }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    sealed interface State {
        data object CheckingWebViewAvailability : State

        data class Show(
            val url: String,
            val copyMessageToClipboard: String? = null,
        ) : State
    }

    sealed interface Event {
        data object BackClicked : Event

        data object OpenExternal : Event

        data object ShareUrl : Event

        data object MessageCopied : Event

        data class OutsideLinkClicked(
            val url: String,
        ) : Event
    }
}
