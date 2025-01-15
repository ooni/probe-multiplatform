package org.ooni.probe.ui.descriptor.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorUpdatesStatus
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.toDescriptor
import org.ooni.probe.domain.SaveTestDescriptors

class ReviewUpdatesViewModel(
    private val onBack: () -> Unit,
    saveTestDescriptors: suspend (List<InstalledTestDescriptorModel>, SaveTestDescriptors.Mode) -> Unit,
    cancelUpdates: (List<InstalledTestDescriptorModel>) -> Unit,
    observeAvailableUpdatesState: () -> Flow<DescriptorUpdatesStatus>,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        observeAvailableUpdatesState()
            .onEach {
                _state.value = _state.value.copy(descriptors = it.reviewUpdates.toList())
            }
            .launchIn(viewModelScope)

        events.onEach {
            when (it) {
                is Event.CancelClicked -> {
                    val state = _state.value
                    cancelUpdates(
                        state.descriptors.subList(
                            state.currentDescriptorIndex,
                            state.descriptors.size,
                        ),
                    )
                    onBack()
                }

                is Event.UpdateDescriptorClicked -> {
                    if (it.index <= _state.value.descriptors.size) {
                        val descriptor = _state.value.descriptors[it.index]
                        saveTestDescriptors(
                            listOf(descriptor),
                            SaveTestDescriptors.Mode.CreateOrUpdate,
                        )
                        navigateToNextItemOrClose(it.index)
                    } else {
                        onBack()
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun navigateToNextItemOrClose(index: Int) {
        val nextIndex = index + 1
        if (nextIndex < _state.value.descriptors.size) {
            _state.value = _state.value.copy(currentDescriptorIndex = nextIndex)
        } else {
            onBack()
        }
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val descriptors: List<InstalledTestDescriptorModel> = emptyList(),
        val currentDescriptorIndex: Int = 0,
    ) {
        val currentDescriptor: Descriptor?
            get() = descriptors.getOrNull(currentDescriptorIndex)?.toDescriptor()
    }

    sealed class Event {
        data object CancelClicked : Event()

        data class UpdateDescriptorClicked(val index: Int) : Event()
    }
}
