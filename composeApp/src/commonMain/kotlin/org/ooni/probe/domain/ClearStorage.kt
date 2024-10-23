package org.ooni.probe.domain

import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class ClearStorage(
    private val backgroundContext: CoroutineContext,
    private val deleteAllResults: suspend () -> Unit,
    private val updateStorageUsed: suspend () -> Unit,
    private val clearLogs: suspend () -> Unit,
) {
    suspend operator fun invoke() {
        withContext(backgroundContext) {
            deleteAllResults()
            clearLogs()
            updateStorageUsed()
        }
    }
}
