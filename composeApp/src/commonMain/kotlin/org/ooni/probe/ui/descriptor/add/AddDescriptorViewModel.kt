package org.ooni.probe.ui.descriptor.add

import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ooni.engine.Engine
import org.ooni.engine.models.Result
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.toDescriptor
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.domain.SaveTestDescriptors
import org.ooni.probe.ui.shared.SelectableItem

class AddDescriptorViewModel(
    onBack: () -> Unit,
    fetchDescriptor: suspend () -> Result<InstalledTestDescriptorModel?, Engine.MkException>,
    private val saveTestDescriptors: suspend (List<InstalledTestDescriptorModel>, SaveTestDescriptors.Mode) -> Unit,
    private val preferenceRepository: PreferenceRepository,
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
            }.onFailure { error ->
                Logger.e(error) { "Failed to fetch descriptor" }
                _state.update { it.copy(messages = it.messages + SnackBarMessage.AddDescriptorFailed) }
                onBack()
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
                    _state.update { it.copy(messages = it.messages + SnackBarMessage.AddDescriptorCancel) }
                    onBack()
                }

                is Event.InstallDescriptorClicked -> {
                    val descriptor = state.value.descriptor ?: return@onEach
                    val selectedTests =
                        state.value.selectableItems.filter { it.isSelected }.map { it.item }
                    installDescriptor(descriptor, selectedTests)
                    _state.update {
                        it.copy(messages = it.messages + SnackBarMessage.AddDescriptorSuccess)
                    }
                    onBack()
                }

                is Event.MessageDisplayed -> {
                    _state.update { it.copy(messages = state.value.messages - event.message) }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private suspend fun installDescriptor(
        descriptor: InstalledTestDescriptorModel,
        selectedTests: List<NetTest>,
    ) {
        saveTestDescriptors(
            listOf(descriptor.copy(autoUpdate = state.value.autoUpdate)),
            SaveTestDescriptors.Mode.CreateOrUpdate,
        )
        preferenceRepository.setAreNetTestsEnabled(
            selectedTests.map { test -> descriptor.toDescriptor() to test },
            isAutoRun = true,
            isEnabled = true,
        )
    }

    data class State(
        val descriptor: InstalledTestDescriptorModel? = null,
        val selectableItems: List<SelectableItem<NetTest>> = emptyList(),
        val messages: List<SnackBarMessage> = emptyList(),
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

        data class MessageDisplayed(val message: SnackBarMessage) : Event
    }

    sealed interface SnackBarMessage {
        data object AddDescriptorFailed : SnackBarMessage

        data object AddDescriptorCancel : SnackBarMessage

        data object AddDescriptorSuccess : SnackBarMessage
    }
}
