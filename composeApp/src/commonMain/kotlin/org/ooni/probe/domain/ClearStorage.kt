package org.ooni.probe.domain

import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class ClearStorage(
    private val backgroundContext: CoroutineContext,
    private val deleteAllResults: suspend () -> Unit,
    private val updateStorageUsed: suspend () -> Unit,
    private val clearLogs: suspend () -> Unit,
    private val clearPreferences: suspend () -> Unit,
) {
    suspend operator fun invoke(fullReset: Boolean = false) {
        withContext(backgroundContext) {
            deleteAllResults()
            clearLogs()
            updateStorageUsed()
            if (fullReset) {
                clearPreferences()
            }
        }
    }
}
