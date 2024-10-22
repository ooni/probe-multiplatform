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
import org.ooni.probe.data.models.ProxySettings
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.TestRunError
import org.ooni.probe.data.models.TestRunState
import org.ooni.probe.data.repositories.PreferenceRepository

class RunningViewModel(
    onBack: () -> Unit,
    goToResults: () -> Unit,
    observeTestRunState: Flow<TestRunState>,
    observeTestRunErrors: Flow<TestRunError>,
    cancelTestRun: () -> Unit,
    preferencesRepository: PreferenceRepository,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        preferencesRepository.allSettings(
            listOf(
                SettingsKey.PROXY_PROTOCOL,
                SettingsKey.PROXY_HOSTNAME,
                SettingsKey.PROXY_PORT,
            ),
        ).onEach { proxySettings ->
            val proxy = ProxySettings.newProxySettings(
                protocol = proxySettings[SettingsKey.PROXY_PROTOCOL] as? String,
                hostname = proxySettings[SettingsKey.PROXY_HOSTNAME] as? String,
                port = proxySettings[SettingsKey.PROXY_PORT] as? String,
            ).getProxyString()
            _state.update { it.copy(hasProxy = proxy.isNotEmpty()) }
        }.launchIn(viewModelScope)
        observeTestRunState
            .onEach { testRunState ->
                if (testRunState is TestRunState.Idle) {
                    if (testRunState.justFinishedTest) {
                        goToResults()
                    } else {
                        onBack()
                    }
                    return@onEach
                }
                _state.update { it.copy(testRunState = testRunState) }
            }
            .launchIn(viewModelScope)

        observeTestRunErrors
            .onEach { error ->
                _state.update { it.copy(testRunErrors = it.testRunErrors + error) }
            }
            .launchIn(viewModelScope)

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
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val testRunState: TestRunState? = null,
        val testRunErrors: List<TestRunError> = emptyList(),
        val hasProxy: Boolean = false,
    )

    sealed interface Event {
        data object BackClicked : Event

        data object StopTestClicked : Event

        data class ErrorDisplayed(val error: TestRunError) : Event
    }
}
