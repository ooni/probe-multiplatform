package org.ooni.probe.shared.monitoring

import androidx.annotation.VisibleForTesting
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import okio.Path.Companion.toPath
import org.ooni.probe.data.disk.AppendFile
import org.ooni.probe.data.disk.DeleteFiles
import org.ooni.probe.data.disk.ReadFile
import org.ooni.probe.data.disk.WriteFile
import org.ooni.probe.shared.now
import org.ooni.probe.ui.shared.logFormat
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AppLogger(
    private val readFile: ReadFile,
    private val writeFile: WriteFile,
    private val appendFile: AppendFile,
    private val deleteFiles: DeleteFiles,
    private val backgroundContext: CoroutineContext,
) {
    private val incoming = Channel<String>(Channel.UNLIMITED)
    private val buffer = ArrayDeque<String>(MAX_LINES)
    private val bufferMutex = Mutex()
    private val log = MutableStateFlow(emptyList<String>())

    fun read(severity: Severity?): Flow<List<String>> =
        log.map { lines ->
            when (severity) {
                null -> lines
                else -> lines.filter { it.contains(": ${severity.name.uppercase()} :") }
            }
        }

    suspend fun clear() {
        bufferMutex.withLock {
            buffer.clear()
            log.value = emptyList()
        }
        deleteFiles(FILE_PATH)
    }

    suspend fun getLogFilePath() =
        FILE_PATH
            .also { appendFile(it, "") } // Ensure file exists

    // Long-running: drains incoming log lines into the in-memory buffer,
    // and persists the log into the log file periodically
    suspend fun start() {
        withContext(backgroundContext) {
            // Initial state from disk log file
            bufferMutex.withLock {
                if (buffer.isEmpty()) {
                    readLogFileLines()
                        .forEach(buffer::addLast)
                }
                // Lines logged before this point are still queued in the channel
                log.value = buffer.toList()
            }

            // Write to file periodically
            launch {
                log
                    .sample(WRITE_TO_FILE_INTERVAL)
                    .distinctUntilChanged()
                    .collectLatest { lines ->
                        writeFile(FILE_PATH, lines.joinToString("\n"))
                    }
            }

            // Process incoming lines in batches and save them to log
            for (first in incoming) {
                bufferMutex.withLock {
                    // Append all lines waiting since the PUBLISH_INTERVAL
                    append(first)
                    while (true) {
                        append(incoming.tryReceive().getOrNull() ?: break)
                    }
                    // One snapshot copy per batch, instead of per log line
                    log.value = buffer.toList()
                }
                // Let the next batch accumulate
                delay(PUBLISH_INTERVAL)
            }
        }
    }

    private suspend fun readLogFileLines(): List<String> =
        readFile(FILE_PATH)
            .orEmpty()
            .lines()
            .filter { it.isNotEmpty() }
            .takeLast(MAX_LINES)

    private fun append(line: String) {
        if (buffer.size >= MAX_LINES) buffer.removeFirst()
        buffer.addLast(line)
    }

    val logWriter = object : LogWriter() {
        override fun isLoggable(
            tag: String,
            severity: Severity,
        ): Boolean = severity != Severity.Verbose

        override fun log(
            severity: Severity,
            message: String,
            tag: String,
            throwable: Throwable?,
        ) {
            incoming.trySend(
                "${LocalDateTime.now().logFormat()} : ${severity.name.uppercase()} : $message",
            )
        }
    }

    companion object {
        val FILE_PATH = "Log/logger.txt".toPath()
        private const val MAX_LINES = 10_000

        @VisibleForTesting
        val WRITE_TO_FILE_INTERVAL = 5.seconds

        @VisibleForTesting
        val PUBLISH_INTERVAL = 500.milliseconds
    }
}
