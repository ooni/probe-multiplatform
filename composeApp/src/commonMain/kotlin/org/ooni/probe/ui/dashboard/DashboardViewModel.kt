package org.ooni.probe.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import org.ooni.probe.config.BatteryOptimization
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorType
import org.ooni.probe.data.models.DescriptorUpdateOperationState
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.data.models.TestRunError

class DashboardViewModel(
    goToOnboarding: () -> Unit,
    goToResults: () -> Unit,
    goToRunningTest: () -> Unit,
    goToRunTests: () -> Unit,
    goToDescriptor: (String) -> Unit,
    getFirstRun: () -> Flow<Boolean>,
    goToReviewDescriptorUpdates: (List<InstalledTestDescriptorModel.Id>?) -> Unit,
    getTestDescriptors: () -> Flow<List<Descriptor>>,
    observeRunBackgroundState: Flow<RunBackgroundState>,
    observeTestRunErrors: Flow<TestRunError>,
    shouldShowVpnWarning: suspend () -> Boolean,
    observeDescriptorUpdateState: () -> Flow<DescriptorsUpdateState>,
    startDescriptorsUpdates: suspend (List<InstalledTestDescriptorModel>?) -> Unit,
    dismissDescriptorsUpdateNotice: () -> Unit,
    getAutoRunSettings: () -> Flow<AutoRunParameters>,
    batteryOptimization: BatteryOptimization,
    canPullToRefresh: Boolean,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State(canPullToRefresh = canPullToRefresh))
    val state = _state.asStateFlow()

    init {
        getFirstRun()
            .take(1)
            .onEach { firstRun -> if (firstRun) goToOnboarding() }
            .launchIn(viewModelScope)

        getAutoRunSettings()
            .take(1)
            .onEach { autoRunParameters ->
                if (autoRunParameters is AutoRunParameters.Enabled &&
                    batteryOptimization.isSupported &&
                    !batteryOptimization.isIgnoring
                ) {
                    _state.update { it.copy(showIgnoreBatteryOptimizationNotice = true) }
                }
            }.launchIn(viewModelScope)

        observeDescriptorUpdateState()
            .onEach { updates ->
                _state.update {
                    it.copy(
                        availableUpdates = updates.availableUpdates.toList(),
                        descriptorsUpdateOperationState = updates.operationState,
                    )
                }
            }.launchIn(viewModelScope)

        getTestDescriptors()
            .onEach { tests ->
                _state.update { it.copy(descriptors = tests.groupByType()) }
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
            .filterIsInstance<Event.RunTestsClick>()
            .onEach { goToRunTests() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RunningTestClick>()
            .onEach { goToRunningTest() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SeeResultsClick>()
            .onEach { goToResults() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ErrorDisplayed>()
            .onEach { event ->
                _state.update { it.copy(testRunErrors = it.testRunErrors - event.error) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DescriptorClicked>()
            .onEach { event -> goToDescriptor(event.descriptor.key) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.Start>()
            .onEach {
                _state.update { it.copy(showVpnWarning = shouldShowVpnWarning()) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.FetchUpdatedDescriptors>()
            .onEach { startDescriptorsUpdates(null) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ReviewUpdatesClicked>()
            .onEach {
                dismissDescriptorsUpdateNotice()
                goToReviewDescriptorUpdates(null)
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UpdateDescriptorClicked>()
            .onEach {
                dismissDescriptorsUpdateNotice()
                goToReviewDescriptorUpdates(
                    listOf(
                        (it.descriptor.source as? Descriptor.Source.Installed)?.value?.id
                            ?: return@onEach,
                    ),
                )
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.CancelUpdatesClicked>()
            .onEach { dismissDescriptorsUpdateNotice() }
            .launchIn(viewModelScope)

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

    private fun List<Descriptor>.groupByType() =
        mapOf(
            DescriptorType.Default to filter { it.source is Descriptor.Source.Default },
            DescriptorType.Installed to filter { it.source is Descriptor.Source.Installed },
        )

    data class State(
        val descriptors: Map<DescriptorType, List<Descriptor>> = emptyMap(),
        val runBackgroundState: RunBackgroundState = RunBackgroundState.Idle(),
        val testRunErrors: List<TestRunError> = emptyList(),
        val showVpnWarning: Boolean = false,
        val availableUpdates: List<InstalledTestDescriptorModel> = emptyList(),
        val descriptorsUpdateOperationState: DescriptorUpdateOperationState = DescriptorUpdateOperationState.Idle,
        val showIgnoreBatteryOptimizationNotice: Boolean = false,
        val canPullToRefresh: Boolean = true,
    ) {
        val isRefreshing: Boolean
            get() = descriptorsUpdateOperationState == DescriptorUpdateOperationState.FetchingUpdates

        val isRefreshEnabled: Boolean
            get() = descriptors[DescriptorType.Installed]?.any() == true
    }

    sealed interface Event {
        data object Start : Event

        data object RunTestsClick : Event

        data object RunningTestClick : Event

        data object SeeResultsClick : Event

        data class ErrorDisplayed(
            val error: TestRunError,
        ) : Event

        data class DescriptorClicked(
            val descriptor: Descriptor,
        ) : Event

        data class UpdateDescriptorClicked(
            val descriptor: Descriptor,
        ) : Event

        data object FetchUpdatedDescriptors : Event

        data object ReviewUpdatesClicked : Event

        data object CancelUpdatesClicked : Event

        data object IgnoreBatteryOptimizationAccepted : Event

        data object IgnoreBatteryOptimizationDismissed : Event
    }
}
