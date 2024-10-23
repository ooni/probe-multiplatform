package org.ooni.probe.background

import co.touchlab.kermit.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ooni.probe.data.models.RunSpecification
import platform.Foundation.NSOperation

class RunOperation(
    private val runBackgroundTask: (RunSpecification?) -> Flow<RunBackgroundTask.State>,
    private val spec: RunSpecification? = null,
) : NSOperation() {
    override fun main() {
        Logger.d { "Running operation" }
        GlobalScope.launch {
            runBackgroundTask(spec).collect()
        }.invokeOnCompletion {
            Logger.d { "Operation completed" }
        }
    }
}
