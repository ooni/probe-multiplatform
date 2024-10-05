package org.ooni.probe.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ClearStorage(
    private val backgroundDispatcher: CoroutineDispatcher,
    private val deleteAllResults: suspend () -> Unit,
    private val getStorageUsed: () -> Unit,
    private val clearLogs: suspend () -> Unit,
) {
    operator fun invoke() {
        CoroutineScope(backgroundDispatcher).launch {
            deleteAllResults()
            clearLogs()
            // TODO: delete logs
            // clear database
            getStorageUsed()
        }
    }
}
