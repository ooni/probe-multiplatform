package org.ooni.probe.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.probe.data.models.RunBackgroundState

class BottomBarViewModel(
    countAllNotViewedFlow: () -> Flow<Long>,
    runBackgroundStateFlow: () -> Flow<RunBackgroundState>,
) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        countAllNotViewedFlow()
            .onEach { count ->
                _state.update { it.copy(notViewedCount = count) }
            }
            .launchIn(viewModelScope)

        runBackgroundStateFlow()
            .onEach { runState ->
                _state.update { it.copy(areTestsRunning = runState !is RunBackgroundState.Idle) }
            }
            .launchIn(viewModelScope)
    }

    data class State(
        val notViewedCount: Long = 0L,
        val areTestsRunning: Boolean = false,
    )
}
