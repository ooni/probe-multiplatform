package org.ooni.probe.ui.descriptor.add

import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.ooni.engine.Engine
import org.ooni.engine.models.Result
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.toDescriptorItem
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.domain.descriptors.SaveTestDescriptors
import org.ooni.probe.shared.now
import org.ooni.probe.ui.shared.SelectableItem

class AddDescriptorViewModel(
    onBack: () -> Unit,
    fetchDescriptor: suspend () -> Result<Descriptor?, Engine.MkException>,
    private val saveTestDescriptors: suspend (List<Descriptor>, SaveTestDescriptors.Mode) -> Unit,
    private val preferenceRepository: PreferenceRepository,
    private val startBackgroundRun: (RunSpecification) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            fetchDescriptor()
                .onSuccess { descriptor ->
                    val descriptor = descriptor ?: return@onSuccess
                    _state.value = State(
                        descriptor = descriptor,
                        selectableItems = descriptor.netTests
                            ?.map { nettest ->
                                SelectableItem(
                                    item = nettest,
                                    isSelected = true,
                                )
                            }.orEmpty(),
                    )
                }.onFailure { error ->
                    Logger.i("Failed to fetch descriptor", error)
                    _state.update {
                        it.copy(messages = it.messages + Message.FailedToFetch)
                    }
                    onBack()
                }
        }

        events
            .filterIsInstance<Event.SelectableItemClicked>()
            .onEach { event ->
                _state.update { state ->
                    val newItems = state.selectableItems.map { item ->
                        if (item == event.item) {
                            item.copy(isSelected = !item.isSelected)
                        } else {
                            item
                        }
                    }
                    state.copy(selectableItems = newItems)
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.AutoUpdateChanged>()
            .onEach { event -> _state.update { it.copy(autoUpdate = event.autoUpdate) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.AutoRunChanged>()
            .onEach { event ->
                _state.update {
                    it.copy(
                        selectableItems = state.value.selectableItems
                            .map { item -> item.copy(isSelected = event.autoRun) },
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.CancelClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.InstallClicked>()
            .onEach { event ->
                installDescriptorAndSavePreferences()
                _state.update {
                    it.copy(messages = it.messages + Message.AddDescriptorSuccess)
                }
                onBack()
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RunClicked>()
            .onEach { event ->
                val installedDescriptor = state.value.descriptor ?: return@onEach
                installDescriptorAndSavePreferences()
                startBackgroundRun(
                    RunSpecification.buildForDescriptor(installedDescriptor.toDescriptorItem()),
                )
                _state.update {
                    it.copy(messages = it.messages + Message.AddDescriptorSuccess)
                }
                onBack()
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.MessageDisplayed>()
            .onEach { event ->
                _state.update { it.copy(messages = state.value.messages - event.message) }
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private suspend fun installDescriptorAndSavePreferences() {
        val descriptor = state.value.descriptor ?: return
        saveTestDescriptors(
            listOf(
                descriptor.copy(
                    autoUpdate = state.value.autoUpdate,
                    dateInstalled = LocalDateTime.now(),
                ),
            ),
            SaveTestDescriptors.Mode.CreateOrUpdate,
        )

        val selectedTests = state.value.selectableItems
            .filter { it.isSelected }
            .map { it.item }
        preferenceRepository.setAreNetTestsEnabled(
            selectedTests.map { test -> descriptor.toDescriptorItem() to test },
            isAutoRun = true,
            isEnabled = true,
        )
    }

    data class State(
        val descriptor: Descriptor? = null,
        val selectableItems: List<SelectableItem<NetTest>> = emptyList(),
        val messages: List<Message> = emptyList(),
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

        data object InstallClicked : Event

        data object RunClicked : Event

        data class SelectableItemClicked(
            val item: SelectableItem<NetTest>,
        ) : Event

        data class AutoUpdateChanged(
            val autoUpdate: Boolean,
        ) : Event

        data class AutoRunChanged(
            val autoRun: Boolean,
        ) : Event

        data class MessageDisplayed(
            val message: Message,
        ) : Event
    }

    enum class Message {
        FailedToFetch,
        AddDescriptorSuccess,
    }
}
