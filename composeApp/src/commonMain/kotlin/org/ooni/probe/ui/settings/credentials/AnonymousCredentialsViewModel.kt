package org.ooni.probe.ui.settings.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ooni.probe.domain.credentials.AnonymousCredentialsHealth

class AnonymousCredentialsViewModel(
    onBack: () -> Unit,
    private val getHealth: suspend () -> AnonymousCredentialsHealth,
    private val clearCredential: suspend () -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        refresh()

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ResetConfirmed>()
            .onEach {
                _state.update { state -> state.copy(isResetting = true) }
                clearCredential()
                _state.update { state -> state.copy(isResetting = false) }
                refresh()
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { state -> state.copy(isLoading = true) }
            _state.update { state -> state.copy(health = getHealth(), isLoading = false) }
        }
    }

    data class State(
        val health: AnonymousCredentialsHealth? = null,
        val isLoading: Boolean = true,
        val isResetting: Boolean = false,
    )

    sealed interface Event {
        data object BackClicked : Event

        data object ResetConfirmed : Event
    }
}
