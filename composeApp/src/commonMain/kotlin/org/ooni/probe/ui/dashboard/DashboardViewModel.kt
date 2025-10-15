package org.ooni.probe.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import org.ooni.probe.config.BatteryOptimization
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.data.models.TestRunError
import org.ooni.probe.shared.tickerFlow
import kotlin.time.Duration.Companion.seconds

class DashboardViewModel(
    goToOnboarding: () -> Unit,
    goToResults: () -> Unit,
    goToRunningTest: () -> Unit,
    goToRunTests: () -> Unit,
    goToTestSettings: () -> Unit,
    getFirstRun: () -> Flow<Boolean>,
    observeRunBackgroundState: Flow<RunBackgroundState>,
    observeTestRunErrors: Flow<TestRunError>,
    shouldShowVpnWarning: suspend () -> Boolean,
    getAutoRunSettings: () -> Flow<AutoRunParameters>,
    batteryOptimization: BatteryOptimization,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        getFirstRun()
            .take(1)
            .onEach { firstRun -> if (firstRun) goToOnboarding() }
            .launchIn(viewModelScope)

        getAutoRunSettings()
            .onEach { autoRunParameters ->
                _state.update {
                    it.copy(isAutoRunEnabled = autoRunParameters is AutoRunParameters.Enabled)
                }
            }.launchIn(viewModelScope)

        getAutoRunSettings()
            .take(1)
            .onEach { autoRunParameters ->
                _state.update {
                    it.copy(
                        showIgnoreBatteryOptimizationNotice =
                            autoRunParameters is AutoRunParameters.Enabled &&
                                batteryOptimization.isSupported &&
                                !batteryOptimization.isIgnoring,
                    )
                }
            }.launchIn(viewModelScope)

        observeRunBackgroundState
            .onEach { testState ->
                _state.update { it.copy(runBackgroundState = testState) }
            }.launchIn(viewModelScope)

        observeTestRunErrors
            .onEach { error ->
                _state.update { it.copy(testRunErrors = it.testRunErrors + error) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RunTestsClicked>()
            .onEach { goToRunTests() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RunningTestClicked>()
            .onEach { goToRunningTest() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.AutoRunClicked>()
            .onEach { goToTestSettings() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SeeResultsClicked>()
            .onEach { goToResults() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ErrorDisplayed>()
            .onEach { event ->
                _state.update { it.copy(testRunErrors = it.testRunErrors - event.error) }
            }.launchIn(viewModelScope)

        merge(
            events.filterIsInstance<Event.Resumed>(),
            events.filterIsInstance<Event.Paused>(),
        ).flatMapLatest {
            if (it is Event.Resumed) {
                tickerFlow(CHECK_VPN_WARNING_INTERVAL)
            } else {
                emptyFlow()
            }
        }.onEach {
            _state.update { it.copy(showVpnWarning = shouldShowVpnWarning()) }
        }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.IgnoreBatteryOptimizationAccepted>()
            .onEach {
                _state.update { it.copy(showIgnoreBatteryOptimizationNotice = false) }
                if (batteryOptimization.isSupported && !batteryOptimization.isIgnoring) {
                    batteryOptimization.requestIgnore()
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.IgnoreBatteryOptimizationDismissed>()
            .onEach { _state.update { it.copy(showIgnoreBatteryOptimizationNotice = false) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val runBackgroundState: RunBackgroundState = RunBackgroundState.Idle(),
        val isAutoRunEnabled: Boolean = false,
        val testRunErrors: List<TestRunError> = emptyList(),
        val showVpnWarning: Boolean = false,
        val showIgnoreBatteryOptimizationNotice: Boolean = false,
    )

    sealed interface Event {
        data object Resumed : Event

        data object Paused : Event

        data object RunTestsClicked : Event

        data object RunningTestClicked : Event

        data object AutoRunClicked : Event

        data object SeeResultsClicked : Event

        data class ErrorDisplayed(
            val error: TestRunError,
        ) : Event

        data object IgnoreBatteryOptimizationAccepted : Event

        data object IgnoreBatteryOptimizationDismissed : Event
    }

    companion object {
        private val CHECK_VPN_WARNING_INTERVAL = 5.seconds
    }
}
