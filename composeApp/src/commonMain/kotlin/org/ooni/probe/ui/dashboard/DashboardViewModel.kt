package org.ooni.probe.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.engine.Engine
import org.ooni.engine.TaskSettings

class DashboardViewModel(
    private val engine: Engine,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        events
            .flatMapLatest { event ->
                when (event) {
                    Event.StartClick -> {
                        if (_state.value.isRunning) return@flatMapLatest emptyFlow()

                        _state.value = _state.value.copy(isRunning = true)

                        engine.startTask(TASK_SETTINGS)
                            .onEach { taskEvent ->
                                _state.update { state ->
                                    state.copy(log = state.log + "\n" + taskEvent)
                                }
                            }
                            .onCompletion {
                                _state.update { it.copy(isRunning = false) }
                            }
                    }
                }
            }
            /*
             This is only needed for this example. The best practice is for the data layer to
             switch to a background dispatcher whenever is needed, and the viewModel should run
             on the default (Main) dispatcher.
             */
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val isRunning: Boolean = false,
        val log: String = "",
    )

    sealed interface Event {
        data object StartClick : Event
    }

    companion object {
        val TASK_SETTINGS =
            TaskSettings(
                name = "web_connectivity",
                inputs = listOf("https://ooni.org"),
                logLevel = "DEBUG2",
            )
    }
}
