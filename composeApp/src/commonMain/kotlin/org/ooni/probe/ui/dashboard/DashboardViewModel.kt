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
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorType
import org.ooni.probe.data.models.DescriptorUpdatesStatus
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.TestRunError
import org.ooni.probe.data.models.TestRunState
import org.ooni.probe.data.models.UpdateStatusType

class DashboardViewModel(
    goToOnboarding: () -> Unit,
    goToResults: () -> Unit,
    goToRunningTest: () -> Unit,
    goToRunTests: () -> Unit,
    goToDescriptor: (String) -> Unit,
    getFirstRun: () -> Flow<Boolean>,
    goToReviewDescriptorUpdates: () -> Unit,
    getTestDescriptors: () -> Flow<List<Descriptor>>,
    observeTestRunState: Flow<TestRunState>,
    observeTestRunErrors: Flow<TestRunError>,
    shouldShowVpnWarning: suspend () -> Boolean,
    fetchDescriptorUpdate: suspend (List<InstalledTestDescriptorModel>) -> Unit,
    reviewUpdates: (List<InstalledTestDescriptorModel>) -> Unit,
    observeAvailableUpdatesState: () -> Flow<DescriptorUpdatesStatus>,
    cancelUpdates: (List<InstalledTestDescriptorModel>) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        getFirstRun()
            .take(1)
            .onEach { firstRun -> if (firstRun) goToOnboarding() }
            .launchIn(viewModelScope)

        observeAvailableUpdatesState().onEach { updates ->
            _state.update {
                it.copy(
                    availableUpdates = updates.availableUpdates.toList(),
                    refreshType = updates.refreshType,
                )
            }
        }.launchIn(viewModelScope)
        getTestDescriptors()
            .onEach { tests ->
                _state.update { it.copy(descriptors = tests.groupByType()) }
            }
            .launchIn(viewModelScope)

        observeTestRunState
            .onEach { testState ->
                _state.update { it.copy(testRunState = testState) }
            }
            .launchIn(viewModelScope)

        observeTestRunErrors
            .onEach { error ->
                _state.update { it.copy(testRunErrors = it.testRunErrors + error) }
            }
            .launchIn(viewModelScope)

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
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DescriptorClicked>()
            .onEach { event ->
                goToDescriptor(event.descriptor.key)
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.Start>()
            .onEach {
                _state.update { it.copy(showVpnWarning = shouldShowVpnWarning()) }
            }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.FetchUpdatedDescriptors>().onEach {
            state.value.descriptors[DescriptorType.Installed]
                ?.map { (it.source as Descriptor.Source.Installed).value }
                ?.let { descriptors ->
                    fetchDescriptorUpdate(descriptors)
                }
        }.launchIn(viewModelScope)
        events.filterIsInstance<Event.ReviewUpdatesClicked>().onEach {
            _state.update {
                it.copy(
                    refreshType = UpdateStatusType.None,
                )
            }
            reviewUpdates(state.value.availableUpdates)
            goToReviewDescriptorUpdates()
        }.launchIn(viewModelScope)
        events.filterIsInstance<Event.CancelUpdatesClicked>().onEach {
            cancelUpdates(state.value.availableUpdates)
            _state.update {
                it.copy(
                    refreshType = UpdateStatusType.None,
                )
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

    data class State(
        val descriptors: Map<DescriptorType, List<Descriptor>> = emptyMap(),
        val testRunState: TestRunState = TestRunState.Idle(),
        val testRunErrors: List<TestRunError> = emptyList(),
        val showVpnWarning: Boolean = false,
        val availableUpdates: List<InstalledTestDescriptorModel> = emptyList(),
        val refreshType: UpdateStatusType = UpdateStatusType.None,
    ) {
        val isRefreshing: Boolean
            get() = refreshType != UpdateStatusType.None
    }

    sealed interface Event {
        data object Start : Event

        data object RunTestsClick : Event

        data object RunningTestClick : Event

        data object SeeResultsClick : Event

        data class ErrorDisplayed(val error: TestRunError) : Event

        data class DescriptorClicked(val descriptor: Descriptor) : Event

        data object FetchUpdatedDescriptors : Event

        data object ReviewUpdatesClicked : Event

        data object CancelUpdatesClicked : Event
    }
}
