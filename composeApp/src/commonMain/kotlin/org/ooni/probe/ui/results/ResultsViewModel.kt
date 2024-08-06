package org.ooni.probe.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.data.models.TestResult

class ResultsViewModel(
    goToResult: (TestResult.Id) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state =
        MutableStateFlow(
            State(results = listOf(TestResult(TestResult.Id("123456")))),
        )
    val state = _state.asStateFlow()

    init {
        events
            .filterIsInstance<Event.ResultClick>()
            .onEach { goToResult(it.result.id) }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val results: List<TestResult>,
    )

    sealed interface Event {
        data class ResultClick(
            val result: TestResult,
        ) : Event
    }
}
