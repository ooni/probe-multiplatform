package org.ooni.probe.ui.running

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ooni.probe.data.models.ProxySettings
import org.ooni.probe.data.models.TestRunError
import org.ooni.probe.data.models.RunBackgroundState

class RunningViewModel(
    onBack: () -> Unit,
    goToResults: () -> Unit,
    observeRunBackgroundState: Flow<RunBackgroundState>,
    observeTestRunErrors: Flow<TestRunError>,
    cancelTestRun: () -> Unit,
    getProxySettings: suspend () -> ProxySettings,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val proxy = getProxySettings().getProxyString()
            _state.update { it.copy(hasProxy = proxy.isNotEmpty()) }
        }
        observeRunBackgroundState
            .onEach { testRunState ->
                if (testRunState is RunBackgroundState.Idle) {
                    if (testRunState.justFinishedTest) {
                        goToResults()
                    } else {
                        onBack()
                    }
                    return@onEach
                }
                _state.update { it.copy(runBackgroundState = testRunState) }
            }.launchIn(viewModelScope)

        observeTestRunErrors
            .onEach { error ->
                _state.update { it.copy(testRunErrors = it.testRunErrors + error) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.StopTestClicked>()
            .onEach { cancelTestRun() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ErrorDisplayed>()
            .onEach { event ->
                _state.update { it.copy(testRunErrors = it.testRunErrors - event.error) }
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val runBackgroundState: RunBackgroundState? = null,
        val testRunErrors: List<TestRunError> = emptyList(),
        val hasProxy: Boolean = false,
    )

    sealed interface Event {
        data object BackClicked : Event

        data object StopTestClicked : Event

        data class ErrorDisplayed(
            val error: TestRunError,
        ) : Event
    }
}
