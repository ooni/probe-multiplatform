package org.ooni.probe.shared.monitoring

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import okio.Path.Companion.toPath
import org.ooni.probe.data.disk.DeleteFiles
import org.ooni.probe.data.disk.ReadFile
import org.ooni.probe.data.disk.WriteFile
import org.ooni.probe.shared.now
import org.ooni.probe.ui.shared.logFormat
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class AppLogger(
    private val readFile: ReadFile,
    private val writeFile: WriteFile,
    private val deleteFiles: DeleteFiles,
    private val backgroundContext: CoroutineContext,
) {
    private val log = MutableStateFlow(emptyList<String>())

    fun read(severity: Severity?): Flow<List<String>> =
        log.map { lines ->
            if (severity == null) {
                lines
            } else {
                lines.filter { line ->
                    line.contains(": ${severity.name.uppercase()} :")
                }
            }
        }

    suspend fun clear() {
        withContext(backgroundContext) {
            log.value = emptyList()
            deleteFiles(FILE_PATH)
        }
    }

    suspend fun getLogFilePath() =
        FILE_PATH
            .also { writeFile(it, "", append = true) } // Ensure file exists

    // Persist the log into the log file after a certain period without changes
    suspend fun writeLogsToFile() {
        withContext(backgroundContext) {
            log
                .onStart {
                    if (log.value.isEmpty()) {
                        log.value = readFile(FILE_PATH).orEmpty().lines()
                    }
                }.debounce(5.seconds)
                .collectLatest { lines ->
                    writeFile(FILE_PATH, lines.joinToString("\n"), append = false)
                }
        }
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
            CoroutineScope(backgroundContext).launch {
                val logMessage =
                    "${LocalDateTime.now().logFormat()} : ${severity.name.uppercase()} : $message"
                log.update { lines ->
                    (lines + logMessage).takeLast(MAX_LINES)
                }
            }
        }
    }

    companion object {
        val FILE_PATH = "Log/logger.txt".toPath()
        private const val MAX_LINES = 1000
    }
}
