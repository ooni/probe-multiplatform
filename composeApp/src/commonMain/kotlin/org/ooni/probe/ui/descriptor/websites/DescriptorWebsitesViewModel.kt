package org.ooni.probe.ui.descriptor.websites

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorItem

class DescriptorWebsitesViewModel(
    descriptorId: Descriptor.Id,
    onBack: () -> Unit,
    getTestDescriptor: (Descriptor.Id) -> Flow<DescriptorItem?>,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    init {
        getTestDescriptor(descriptorId)
            .onEach { descriptor ->
                val websites = descriptor
                    ?.allTests
                    ?.firstOrNull { it.test == TestType.WebConnectivity }
                    ?.inputs
                    .orEmpty()

                if (descriptor == null || websites.isEmpty()) {
                    onBack()
                    return@onEach
                }

                _state.value = State.Show(
                    title = descriptor.title,
                    color = descriptor.color,
                    websites = websites,
                )
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    sealed interface State {
        data object Loading : State

        data class Show(
            val title: () -> String,
            val color: Color?,
            val websites: List<String>,
        ) : State
    }

    sealed interface Event {
        data object BackClicked : Event
    }
}
