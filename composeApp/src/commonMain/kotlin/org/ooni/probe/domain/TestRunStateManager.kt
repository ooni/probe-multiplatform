package org.ooni.probe.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.ooni.probe.data.models.TestRunError
import org.ooni.probe.data.models.TestRunState

class TestRunStateManager {
    private val state = MutableStateFlow<TestRunState>(TestRunState.Idle)
    private val cancels = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val errors = MutableSharedFlow<TestRunError>(extraBufferCapacity = 1)

    fun observeState() = state.asStateFlow()

    fun observeTestRunCancels() = cancels.asSharedFlow()

    fun observeError() = errors.asSharedFlow()

    fun updateState(update: (TestRunState) -> TestRunState) = state.update(update)

    fun cancelTestRun() {
        cancels.tryEmit(Unit)
    }

    fun reportError(error: TestRunError) {
        errors.tryEmit(error)
    }
}
