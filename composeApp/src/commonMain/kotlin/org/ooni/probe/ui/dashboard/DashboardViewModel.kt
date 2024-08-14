package org.ooni.probe.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.engine.Engine
import org.ooni.engine.models.TaskEvent
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.Descriptor

class DashboardViewModel(
    private val engine: Engine,
    getTestDescriptors: () -> Flow<List<Descriptor>>,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        getTestDescriptors()
            .onEach { tests ->
                _state.update { it.copy(tests = tests.groupByType()) }
            }
            .launchIn(viewModelScope)

        events
            .flatMapLatest { event ->
                when (event) {
                    Event.StartClick -> runTest()
                }
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private fun List<Descriptor>.groupByType() =
        mapOf(
            DescriptorType.Default to filter { it.source is Descriptor.Source.Default },
            DescriptorType.Installed to filter { it.source is Descriptor.Source.Installed },
        )

    private suspend fun runTest(): Flow<TaskEvent> {
        if (_state.value.isRunning) return emptyFlow()

        _state.value = _state.value.copy(isRunning = true)

        engine.httpDo(
            method = "GET",
            url = "https://api.dev.ooni.io/api/v2/oonirun/links/10426",
        )
            .onSuccess { Logger.d(it.orEmpty()) }
            .onFailure { Logger.e("httpDo failed", it) }

        val checkInResults =
            engine.checkIn(
                categories = listOf("NEWS"),
                taskOrigin = TaskOrigin.OoniRun,
            )
                .onFailure { Logger.e("checkIn failed", it) }
                .get() ?: return emptyFlow()

        return engine
            .startTask(
                name = "web_connectivity",
                inputs = checkInResults.urls.map { it.url },
                taskOrigin = TaskOrigin.OoniRun,
            ).onEach { taskEvent ->
                _state.update { state ->
                    // Can't print the Measurement event,
                    // it's too long and halts the main thread
                    if (taskEvent is TaskEvent.Measurement) return@update state

                    state.copy(log = state.log + "\n" + taskEvent)
                }
            }.onCompletion {
                _state.update { it.copy(isRunning = false) }
            }
            .catch {
                Logger.e("startTask failed", it)
            }
    }

    enum class DescriptorType {
        Default,
        Installed,
    }

    data class State(
        val tests: Map<DescriptorType, List<Descriptor>> = emptyMap(),
        val isRunning: Boolean = false,
        val log: String = "",
    )

    sealed interface Event {
        data object StartClick : Event
    }
}
