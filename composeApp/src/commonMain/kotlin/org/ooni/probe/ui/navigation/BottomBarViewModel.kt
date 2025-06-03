package org.ooni.probe.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.ooni.probe.data.repositories.ResultRepository

class BottomBarViewModel(
    private val resultRepository: ResultRepository,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(
        State(
            notViewedCount = 0L,
        ),
    )
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            resultRepository.countAllNotViewedFlow().collectLatest { count ->
                _state.value = _state.value.copy(notViewedCount = count)
            }
        }

        events
            .filterIsInstance<Event.Refresh>()
            .onEach { refreshNotViewedCount() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private suspend fun refreshNotViewedCount() {
        val count = resultRepository.countAllNotViewedFlow().firstOrNull() ?: 0L
        _state.value = _state.value.copy(notViewedCount = count)
    }

    data class State(
        val notViewedCount: Long,
    )

    sealed interface Event {
        data object Refresh : Event
    }
}
