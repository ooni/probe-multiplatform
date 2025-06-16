package org.ooni.probe.ui.tests

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
import org.ooni.probe.data.models.DescriptorUpdateOperationState
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.InstalledTestDescriptorModel

class TestsViewModel(
    goToDescriptor: (String) -> Unit,
    goToReviewDescriptorUpdates: (List<InstalledTestDescriptorModel.Id>?) -> Unit,
    getTestDescriptors: () -> Flow<List<Descriptor>>,
    observeDescriptorUpdateState: () -> Flow<DescriptorsUpdateState>,
    startDescriptorsUpdates: suspend (List<InstalledTestDescriptorModel>?) -> Unit,
    dismissDescriptorsUpdateNotice: () -> Unit,
    canPullToRefresh: Boolean,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State(canPullToRefresh = canPullToRefresh))
    val state = _state.asStateFlow()

    init {
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

        events
            .filterIsInstance<Event.DescriptorClicked>()
            .onEach { event -> goToDescriptor(event.descriptor.key) }
            .launchIn(viewModelScope)

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
        val availableUpdates: List<InstalledTestDescriptorModel> = emptyList(),
        val descriptorsUpdateOperationState: DescriptorUpdateOperationState = DescriptorUpdateOperationState.Idle,
        val canPullToRefresh: Boolean = true,
    ) {
        val isRefreshing: Boolean
            get() = descriptorsUpdateOperationState == DescriptorUpdateOperationState.FetchingUpdates

        val isRefreshEnabled: Boolean
            get() = descriptors[DescriptorType.Installed]?.any() == true
    }

    sealed interface Event {
        data class DescriptorClicked(
            val descriptor: Descriptor,
        ) : Event

        data class UpdateDescriptorClicked(
            val descriptor: Descriptor,
        ) : Event

        data object FetchUpdatedDescriptors : Event

        data object ReviewUpdatesClicked : Event

        data object CancelUpdatesClicked : Event
    }
}
