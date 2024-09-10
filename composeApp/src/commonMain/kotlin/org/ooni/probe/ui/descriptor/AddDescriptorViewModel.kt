package org.ooni.probe.ui.descriptor

import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.ooni.engine.Engine
import org.ooni.engine.models.Result
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.ui.shared.SelectableItem

class AddDescriptorViewModel(
    onCancel: () -> Unit,
    onError: () -> Unit,
    fetchDescriptor: suspend () -> Result<InstalledTestDescriptorModel?, Engine.MkException>,
    saveTestDescriptors: suspend (List<Pair<InstalledTestDescriptorModel, List<NetTest>>>) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            fetchDescriptor().map { descriptor ->
                descriptor?.let {
                    _state.value = State(
                        descriptor = it,
                        selectableItems = it.netTests?.map { nettest ->
                            SelectableItem(
                                item = nettest,
                                isSelected = true,
                            )
                        }.orEmpty(),
                    )
                }
            }.onFailure {
                Logger.e(it) { "Failed to fetch descriptor" }
                onError()
            }
        }

        events.onEach { event ->
            when (event) {
                is Event.SelectableItemClicked -> {
                    val newItems = state.value.selectableItems.map { item ->
                        if (item == event.item) {
                            item.copy(isSelected = !item.isSelected)
                        } else {
                            item
                        }
                    }
                    _state.value = state.value.copy(selectableItems = newItems)
                }

                is Event.AutoUpdateChanged -> {
                    _state.value = state.value.copy(autoUpdate = event.autoUpdate)
                }

                is Event.AutoRunChanged -> {
                    _state.value =
                        state.value.copy(
                            selectableItems = state.value.selectableItems.map { item ->
                                item.copy(isSelected = event.autoRun)
                            },
                        )
                }

                is Event.CancelClicked -> {
                    onCancel()
                }

                is Event.InstallDescriptorClicked -> {
                    val selectedTests =
                        state.value.selectableItems.filter { it.isSelected }.map { it.item }
                    state.value.descriptor?.let { descriptor ->
                        viewModelScope.launch {
                            saveTestDescriptors(
                                listOf(descriptor.copy(autoUpdate = state.value.autoUpdate) to selectedTests),
                            )
                        }
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val descriptor: InstalledTestDescriptorModel? = null,
        val selectableItems: List<SelectableItem<NetTest>> = emptyList(),
        val autoUpdate: Boolean = true,
    ) {
        fun allTestsSelected(): ToggleableState {
            val selectedTestsCount =
                selectableItems.count { it.isSelected }
            return when (selectedTestsCount) {
                0 -> ToggleableState.Off
                selectableItems.size -> ToggleableState.On
                else -> ToggleableState.Indeterminate
            }
        }
    }

    sealed interface Event {
        data object CancelClicked : Event

        data object InstallDescriptorClicked : Event

        data class SelectableItemClicked(val item: SelectableItem<NetTest>) : Event

        data class AutoUpdateChanged(val autoUpdate: Boolean) : Event

        data class AutoRunChanged(val autoRun: Boolean) : Event
    }
}
