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
import kotlinx.coroutines.flow.update
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorType
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.TestRunError
import org.ooni.probe.data.models.TestRunState
import org.ooni.probe.data.models.UpdateStatusType
import org.ooni.probe.domain.ResultStatus

class DashboardViewModel(
    goToResults: () -> Unit,
    goToRunningTest: () -> Unit,
    goToRunTests: () -> Unit,
    goToDescriptor: (String) -> Unit,
    getTestDescriptors: () -> Flow<List<Descriptor>>,
    observeTestRunState: Flow<TestRunState>,
    observeTestRunErrors: Flow<TestRunError>,
    fetchDescriptorUpdate: suspend (List<InstalledTestDescriptorModel>) -> List<ResultStatus>,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
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
            .onEach { goToDescriptor(it.descriptor.key) }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.FetchUpdatedDescriptors>().onEach {
            _state.update { it.copy(refreshType = UpdateStatusType.UpdateLink) }
            state.value.descriptors[DescriptorType.Installed]
                ?.filter { it.source is Descriptor.Source.Installed }
                ?.map { (it.source as Descriptor.Source.Installed).value }
                ?.let { descriptors ->
                    val results = fetchDescriptorUpdate(descriptors)
                    _state.update { it.copy(refreshType = UpdateStatusType.None) }
                    results.forEach { resultStatus: ResultStatus ->
                        when (resultStatus) {
                            is ResultStatus.AutoUpdated -> println("AutoUpdated")
                            is ResultStatus.NoUpdates -> println("NoUpdates")
                        }
                        resultStatus.value.onSuccess { updatedDescriptor ->

                            println("Updated descriptor: $updatedDescriptor")
                        }.onFailure {
                            println("Failed to fetch updated descriptor: $it")
                            _state.update {
                                it.copy(refreshType = UpdateStatusType.None)
                            }
                        }
                    }
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
        val refreshType: UpdateStatusType = UpdateStatusType.None,
    ) {
        val isRefreshing: Boolean
            get() = refreshType != UpdateStatusType.None
    }

    sealed interface Event {
        data object RunTestsClick : Event

        data object RunningTestClick : Event

        data object SeeResultsClick : Event

        data class ErrorDisplayed(val error: TestRunError) : Event

        data class DescriptorClicked(val descriptor: Descriptor) : Event

        data object FetchUpdatedDescriptors : Event
    }
}
