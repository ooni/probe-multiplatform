package org.ooni.probe.ui.descriptor.review

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
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.DescriptorItem
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.toDescriptorItem

class ReviewUpdatesViewModel(
    private val ids: List<Descriptor.Id>?,
    private val onBack: () -> Unit,
    observeDescriptorsUpdateState: () -> Flow<DescriptorsUpdateState>,
    acceptDescriptorUpdate: suspend (Descriptor) -> Unit,
    rejectDescriptorUpdate: suspend (Descriptor) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        observeDescriptorsUpdateState()
            .take(1)
            .onEach { updateState ->
                val descriptorsToReview = updateState.availableUpdates.filter {
                    ids == null || ids.contains(it.id)
                }
                if (descriptorsToReview.isEmpty()) {
                    onBack()
                    return@onEach
                }
                _state.update { state ->
                    state.copy(descriptors = descriptorsToReview)
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UpdateClicked>()
            .onEach {
                val descriptor = _state.value.currentInstalledDescriptor ?: return@onEach
                acceptDescriptorUpdate(descriptor)
                navigateToNextItemOrClose()
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RejectClicked>()
            .onEach {
                val descriptor = _state.value.currentInstalledDescriptor ?: return@onEach
                rejectDescriptorUpdate(descriptor)
                navigateToNextItemOrClose()
            }.launchIn(viewModelScope)
    }

    private fun navigateToNextItemOrClose() {
        val state = _state.value
        if (state.index + 1 < state.descriptors.size) {
            _state.update { it.copy(index = state.index + 1) }
        } else {
            onBack()
        }
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val descriptors: List<Descriptor> = emptyList(),
        val index: Int = 0,
    ) {
        val currentInstalledDescriptor: Descriptor?
            get() = descriptors.getOrNull(index)
        val currentDescriptor: DescriptorItem?
            get() = descriptors.getOrNull(index)?.toDescriptorItem()
    }

    sealed interface Event {
        data object BackClicked : Event

        data object RejectClicked : Event

        data object UpdateClicked : Event
    }
}
