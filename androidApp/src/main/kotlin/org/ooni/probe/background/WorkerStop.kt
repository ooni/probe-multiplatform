package org.ooni.probe.background

import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.ooni.probe.domain.BackgroundWorkLimit

// Calls [onStopped] as soon as the system stops the worker, without waiting for [work] to unwind
suspend fun <T> CoroutineWorker.runStoppable(
    onStopped: () -> Unit,
    work: suspend () -> T,
): T =
    coroutineScope {
        val stopWatcher = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                if (isStopped) onStopped()
            }
        }
        try {
            work()
        } finally {
            stopWatcher.cancel()
        }
    }

fun CoroutineWorker.stopReasonOrNull(): Int? =
    if (Build.VERSION.SDK_INT >= 31) stopReason else null

fun BackgroundWorkLimit.markReachedIfNeeded(
    workerName: String,
    stopReason: Int?,
) {
    if (stopReason != WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT) return
    // Block so the restarted worker already sees the limit
    runBlocking { markReached() }
    Logger.i("$workerName: background work limit reached")
}
