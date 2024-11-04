package org.ooni.probe.ui.choosewebsites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.ui.shared.isValidUrl

class ChooseWebsitesViewModel(
    onBack: () -> Unit,
    goToDashboard: () -> Unit,
    startBackgroundRun: (RunSpecification) -> Unit,
    initialUrl: String? = null,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(
        State(
            websites = listOfNotNull(
                initialUrl?.let {
                    WebsiteItem(url = it, hasError = it.isValidUrl())
                },
            ),
        ),
    )
    val state = _state.asStateFlow()

    init {
        events
            .filterIsInstance<Event.BackClicked>()
            .onEach {
                if (_state.value == State()) {
                    onBack()
                } else {
                    _state.update { it.copy(showBackConfirmation = true) }
                }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackCancelled>()
            .onEach { _state.update { it.copy(showBackConfirmation = false) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackConfirmed>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.AddWebsiteClicked>()
            .onEach { _state.update { it.copy(websites = it.websites + WebsiteItem()) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DeleteWebsiteClicked>()
            .onEach { event ->
                if (_state.value.websites.size <= 1) return@onEach
                _state.update {
                    val newList = it.websites.filterIndexed { index, _ -> index != event.index }
                    it.copy(websites = newList)
                }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UrlChanged>()
            .onEach { event ->
                _state.update {
                    it.copy(
                        websites = it.websites.toMutableList()
                            .also { list ->
                                list[event.index] = list[event.index]
                                    .copy(url = event.url, hasError = false)
                            }
                            .toList(),
                    )
                }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RunClicked>()
            .onEach {
                val items = _state.value.websites.map { item ->
                    item.copy(hasError = !item.url.isValidUrl())
                }

                if (items.any { it.hasError }) {
                    _state.value = _state.value.copy(websites = items)
                    return@onEach
                }

                startBackgroundRun(
                    RunSpecification(
                        tests = listOf(
                            RunSpecification.Test(
                                source = RunSpecification.Test.Source.Default("websites"),
                                netTests = listOf(
                                    NetTest(
                                        test = TestType.WebConnectivity,
                                        inputs = items.map { it.url },
                                    ),
                                ),
                            ),
                        ),
                        taskOrigin = TaskOrigin.OoniRun,
                        isRerun = false,
                    ),
                )
                goToDashboard()
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class WebsiteItem(
        val url: String = "http://",
        val hasError: Boolean = false,
    )

    data class State(
        val websites: List<WebsiteItem> = listOf(WebsiteItem()),
        val showBackConfirmation: Boolean = false,
    ) {
        val canRemoveUrls get() = websites.size > 1
    }

    sealed interface Event {
        data object BackClicked : Event

        data object BackConfirmed : Event

        data object BackCancelled : Event

        data class UrlChanged(val index: Int, val url: String) : Event

        data class DeleteWebsiteClicked(val index: Int) : Event

        data object AddWebsiteClicked : Event

        data object RunClicked : Event
    }
}
