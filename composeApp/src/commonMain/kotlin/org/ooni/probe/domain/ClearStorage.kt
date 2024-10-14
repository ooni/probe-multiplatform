package org.ooni.probe.domain

import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class ClearStorage(
    private val backgroundContext: CoroutineContext,
    private val deleteAllResults: suspend () -> Unit,
    private val getStorageUsed: suspend () -> Unit,
    private val clearLogs: suspend () -> Unit,
) {
    suspend operator fun invoke() {
        withContext(backgroundContext) {
            deleteAllResults()
            clearLogs()
            getStorageUsed()
        }
    }
}
