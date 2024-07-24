package org.ooni.probe.ui.main

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.engine.Engine
import org.ooni.engine.TaskSettings

class MainViewModel(
    private val engine: Engine
) {
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
            .launchIn(CoroutineScope(Dispatchers.IO))
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val isRunning: Boolean = false,
        val log: String = ""
    )

    sealed interface Event {
        data object StartClick : Event
    }

    companion object {
        val TASK_SETTINGS = TaskSettings(
            name = "web_connectivity",
            inputs = listOf("https://ooni.org"),
            logLevel = "DEBUG2"
        )
    }
}
