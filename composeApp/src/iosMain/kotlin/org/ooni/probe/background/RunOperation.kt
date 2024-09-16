package org.ooni.probe.background

import co.touchlab.kermit.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.TestRunState
import org.ooni.probe.domain.GetAutoRunSpecification
import org.ooni.probe.domain.RunDescriptors
import platform.Foundation.NSOperation

class RunOperation(
    private val spec: RunSpecification? = null,
    private val getAutoRunSpecification: GetAutoRunSpecification? = null,
    private val runDescriptors: RunDescriptors,
    private val getCurrentTestState: () -> Flow<TestRunState>,
) : NSOperation() {
    init {
        require(spec != null || getAutoRunSpecification != null) {
            "Either spec or getAutoRunSpecification must be provided"
        }
    }

    override fun main() {
        Logger.d { "Running operation" }
        GlobalScope.launch {
            coroutineScope {
                val runJob = async {
                    runDescriptors(getSpecification()!!)
                }
                // Observe the run state to update the notifications and finish the worker when it's done
                var testStarted = false
                getCurrentTestState().takeWhile { state ->
                    state is TestRunState.Running || (state is TestRunState.Idle && !testStarted)
                }.onEach { state ->
                    if (state !is TestRunState.Running) return@onEach
                    testStarted = true
                }.collect()

                runJob.await()
            }
        }.invokeOnCompletion {
            Logger.d { "Operation completed" }
        }
    }

    private suspend fun getSpecification(): RunSpecification? {
        return spec ?: getAutoRunSpecification?.invoke()
    }
}
