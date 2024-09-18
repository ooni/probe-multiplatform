package org.ooni.probe.ui.dashboard

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.Result
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorType
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.TestRunError
import org.ooni.probe.data.models.TestRunState
import org.ooni.probe.data.models.UpdateStatus
import org.ooni.probe.data.models.UpdateStatusType
import org.ooni.probe.domain.ResultStatus
import kotlin.reflect.KFunction0

class DashboardViewModel(
    goToResults: () -> Unit,
    goToRunningTest: () -> Unit,
    goToRunTests: () -> Unit,
    goToDescriptor: (String) -> Unit,
    getTestDescriptors: () -> Flow<List<Descriptor>>,
    observeTestRunState: Flow<TestRunState>,
    observeTestRunErrors: Flow<TestRunError>,
    fetchDescriptorUpdate: suspend (
        List<InstalledTestDescriptorModel>,
    ) -> MutableMap<ResultStatus, MutableList<Result<InstalledTestDescriptorModel?, MkException>>>,
    reviewUpdates: (List<InstalledTestDescriptorModel>) -> Unit,
    observeAvailableUpdatesState: () -> Flow<Set<InstalledTestDescriptorModel>>,
    cancelUpdates: (Set<InstalledTestDescriptorModel>) -> Unit,
    observeCanceledUpdatesState: KFunction0<StateFlow<Set<InstalledTestDescriptorModel>>>,
) : ViewModel(), DefaultLifecycleObserver {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        observeAvailableUpdatesState().onEach { updates ->
            _state.update {
                it.copy(
                    availableUpdates = updates.toList(),
                )
            }

            if (updates.isNotEmpty()) {
                _state.update { it.copy(refreshType = UpdateStatusType.ReviewLink) }
            }
        }.launchIn(viewModelScope)
        observeCanceledUpdatesState().onEach { updates ->
            _state.update {
                it.copy(
                    descriptors = it.descriptors.mapValues { (type, descriptors) ->
                        descriptors.map { descriptor ->
                            descriptor.copy(
                                updateStatus = updates.firstOrNull { it.id.value.toString() == descriptor.key }?.let {
                                    UpdateStatus.UpdateRejected(it)
                                } ?: UpdateStatus.UpToDate,
                            )
                        }
                    },
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

        events.filterIsInstance<Event.FetchUpdatedDescriptors>().onEach {
            state.value.descriptors[DescriptorType.Installed]
                ?.map { (it.source as Descriptor.Source.Installed).value }
                ?.let { descriptors ->
                    _state.update { it.copy(refreshType = UpdateStatusType.UpdateLink) }
                    fetchDescriptorUpdate(descriptors)
                    _state.update { it.copy(refreshType = UpdateStatusType.ReviewLink) }
                }
        }.launchIn(viewModelScope)
        events.filterIsInstance<Event.ReviewUpdatesClicked>().onEach {
            reviewUpdates(state.value.availableUpdates)
            _state.update {
                it.copy(
                    refreshType = UpdateStatusType.None,
                )
            }
        }.launchIn(viewModelScope)
        events.filterIsInstance<Event.CancelUpdatesClicked>().onEach {
            cancelUpdates(state.value.availableUpdates.toSet())
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
        val availableUpdates: List<InstalledTestDescriptorModel> = emptyList(),
        val refreshType: UpdateStatusType = UpdateStatusType.UpdateLink,
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

        data object ReviewUpdatesClicked : Event

        data object CancelUpdatesClicked : Event
    }
}
