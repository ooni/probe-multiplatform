package org.ooni.probe.ui.descriptor.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.InstalledTestDescriptorModel

class AddDescriptorUrlViewModel(
    onClose: () -> Unit,
    goToAddDescriptor: (InstalledTestDescriptorModel.Id) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        events
            .filterIsInstance<Event.CloseClicked>()
            .onEach { onClose() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.InputChanged>()
            .onEach { event ->
                _state.update {
                    it.copy(
                        input = event.value,
                        isInvalid = false,
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.NextClicked>()
            .onEach {
                val input = _state.value.input
                if (!input.isValidInput()) {
                    _state.update { it.copy(isInvalid = true) }
                    return@onEach
                }

                goToAddDescriptor(
                    InstalledTestDescriptorModel.Id(
                        input.removePrefix(RUN_LINK_PREFIX),
                    ),
                )
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private fun String.isValidInput() =
        toLongOrNull() != null ||
            (startsWith(RUN_LINK_PREFIX) && length > RUN_LINK_PREFIX.length)

    data class State(
        val input: String = "",
        val isInvalid: Boolean = false,
    )

    sealed interface Event {
        data object CloseClicked : Event

        data class InputChanged(
            val value: String,
        ) : Event

        data object NextClicked : Event
    }

    companion object {
        val RUN_LINK_PREFIX = "${OrganizationConfig.ooniRunDashboardUrl}/v2/"
    }
}
