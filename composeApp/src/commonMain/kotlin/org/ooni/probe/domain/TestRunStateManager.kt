package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.TestRunError
import org.ooni.probe.data.models.TestRunState

class TestRunStateManager(
    private val getLatestResult: Flow<ResultModel?>,
) {
    private val state = MutableStateFlow<TestRunState>(TestRunState.Idle())
    private val cancels = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val errors = MutableSharedFlow<TestRunError>(extraBufferCapacity = 1)

    fun observeState() =
        state.asStateFlow()
            .onStart {
                state.update { value ->
                    if (value !is TestRunState.Idle || value.lastTestAt != null) return@update value
                    TestRunState.Idle(lastTestAt = getLatestResult.first()?.startTime)
                }
            }

    fun observeCancels() = cancels.asSharedFlow()

    fun observeErrors() = errors.asSharedFlow()

    fun updateState(update: (TestRunState) -> TestRunState) = state.update(update)

    fun cancelTestRun() {
        cancels.tryEmit(Unit)
    }

    fun reportError(error: TestRunError) {
        errors.tryEmit(error)
    }
}
