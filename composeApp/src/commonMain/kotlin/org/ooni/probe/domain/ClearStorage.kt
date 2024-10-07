package org.ooni.probe.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okio.Path
import org.ooni.probe.shared.monitoring.AppLogger
import kotlin.reflect.KSuspendFunction1

class ClearStorage(
    private val backgroundDispatcher: CoroutineDispatcher,
    private val deleteAllResults: suspend () -> Unit,
    private val getStorageUsed: () -> Unit,
    private val clearLogs: suspend () -> Unit,
    private val deleteFiles: KSuspendFunction1<Path, Unit>,
) {
    operator fun invoke() {
        CoroutineScope(backgroundDispatcher).launch {
            deleteAllResults()
            clearLogs()
            deleteFiles(AppLogger.FILE_PATH)
            getStorageUsed()
        }
    }
}
