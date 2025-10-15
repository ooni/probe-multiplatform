package org.ooni.probe.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.data.models.TestRunError

class RunBackgroundStateManager {
    private val state = MutableStateFlow<RunBackgroundState>(RunBackgroundState.Idle)
    private val errors = MutableSharedFlow<TestRunError>(extraBufferCapacity = 1)
    private val cancelListeners = mutableListOf<() -> Unit>()

    // State

    fun observeState() = state.asSharedFlow()

    fun updateState(update: (RunBackgroundState) -> RunBackgroundState) {
        state.update(update)
    }

    // Errors

    fun observeErrors() = errors.asSharedFlow()

    fun reportError(error: TestRunError) {
        errors.tryEmit(error)
    }

    // Cancels

    fun addCancelListener(listener: () -> Unit): CancelListenerCallback {
        cancelListeners.add(listener)
        return CancelListenerCallback { cancelListeners.remove(listener) }
    }

    fun cancel() {
        cancelListeners.forEach { it() }
        cancelListeners.clear()
    }
}

fun interface CancelListenerCallback {
    fun dismiss()
}
