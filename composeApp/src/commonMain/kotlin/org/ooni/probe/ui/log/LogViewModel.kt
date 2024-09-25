package org.ooni.probe.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Severity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class LogViewModel(
    onBack: () -> Unit,
    readLog: (Severity?) -> Flow<List<String>>,
    clearLog: suspend () -> Unit,
    shareLogFile: suspend () -> Boolean,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        state
            .map { it.filter }
            .flatMapLatest { filter -> readLog(filter) }
            .onEach { log -> _state.update { state -> state.copy(log = log) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ClearClicked>()
            .onEach { clearLog() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ShareClicked>()
            .onEach {
                if (!shareLogFile()) {
                    _state.update { it.copy(errors = it.errors + Error.Share) }
                }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.FilterChanged>()
            .onEach { event -> _state.update { it.copy(filter = event.severity) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ErrorShown>()
            .onEach { event -> _state.update { it.copy(errors = it.errors - event.error) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val log: List<String> = emptyList(),
        val filter: Severity? = null,
        val errors: List<Error> = emptyList(),
    )

    sealed interface Event {
        data object BackClicked : Event

        data object ClearClicked : Event

        data object ShareClicked : Event

        data class FilterChanged(val severity: Severity?) : Event

        data class ErrorShown(val error: Error) : Event
    }

    enum class Error { Share }
}
