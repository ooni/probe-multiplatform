package org.ooni.probe.ui.settings.proxy

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
import org.ooni.probe.data.models.ProxyOption
import org.ooni.probe.domain.proxy.TestProxy
import org.ooni.probe.ui.shared.SelectableItem

class ProxyViewModel(
    onBack: () -> Unit,
    goToAddProxy: () -> Unit,
    getProxyOptions: () -> Flow<List<SelectableItem<ProxyOption>>>,
    private val selectProxyOption: suspend (ProxyOption) -> Unit,
    private val deleteProxyOption: suspend (ProxyOption.Custom) -> Unit,
    private val testProxy: (ProxyOption.Custom) -> Flow<TestProxy.State>,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        getProxyOptions()
            .onEach { options ->
                _state.update { state ->
                    state.copy(
                        items = options.map {
                            Item(
                                option = it.item,
                                isSelected = it.isSelected,
                                // Keep existing state, if there's any
                                testState = state.items
                                    .firstOrNull { stateItem -> stateItem.option == it.item }
                                    ?.testState,
                            )
                        },
                    )
                }
                val selectedOption = options.firstOrNull { it.isSelected }?.item
                if (selectedOption is ProxyOption.Custom) {
                    testProxy(selectedOption)
                        .onEach { testState ->
                            _state.update { state ->
                                state.copy(
                                    items = state.items.map {
                                        if (it.option == selectedOption) {
                                            it.copy(testState = testState)
                                        } else {
                                            it
                                        }
                                    },
                                )
                            }
                        }.launchIn(viewModelScope)
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.AddCustomClicked>()
            .onEach { goToAddProxy() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DeleteCustomClicked>()
            .onEach { deleteProxyOption(it.option) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.OptionSelected>()
            .onEach { selectProxyOption(it.option) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val items: List<Item> = emptyList(),
    )

    sealed interface Event {
        data object BackClicked : Event

        data class OptionSelected(
            val option: ProxyOption,
        ) : Event

        data object AddCustomClicked : Event

        data class DeleteCustomClicked(
            val option: ProxyOption.Custom,
        ) : Event
    }

    data class Item(
        val option: ProxyOption,
        val isSelected: Boolean,
        val testState: TestProxy.State?,
    )
}
