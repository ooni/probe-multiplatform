package org.ooni.probe.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ClearStorage(
    private val backgroundDispatcher: CoroutineDispatcher,
    private val deleteAllResults: suspend () -> Unit,
    private val getStorageUsed: suspend () -> Unit,
    private val clearLogs: suspend () -> Unit,
) {
    suspend operator fun invoke() {
        withContext(backgroundDispatcher) {
            deleteAllResults()
            clearLogs()
            getStorageUsed()
        }
    }
}
