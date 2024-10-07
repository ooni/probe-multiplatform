package org.ooni.probe.shared.monitoring

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

class AppLogger(
    private val readFile: ReadFile,
    private val writeFile: WriteFile,
    private val deleteFiles: DeleteFiles,
    private val backgroundDispatcher: CoroutineDispatcher,
) {
    private val log = MutableStateFlow(emptyList<String>())

    fun read(severity: Severity?): Flow<List<String>> =
        log
            .onStart {
                if (log.value.isEmpty()) {
                    log.value = readFile(FILE_PATH).orEmpty().lines()
                }
            }
            .map { lines ->
                if (severity == null) {
                    lines
                } else {
                    lines.filter { line ->
                        line.contains(": ${severity.name.uppercase()} :")
                    }
                }
            }

    suspend fun clear() {
        withContext(backgroundDispatcher) {
            log.value = emptyList()
            deleteFiles(FILE_PATH)
        }
    }

    fun getLogFilePath() = FILE_PATH

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
            CoroutineScope(backgroundDispatcher).launch {
                val logMessage =
                    "${LocalDateTime.now().logFormat()} : ${severity.name.uppercase()} : $message"
                log.update { lines ->
                    val newLines = (lines + logMessage).takeLast(MAX_LINES)
                    writeFile(FILE_PATH, newLines.joinToString("\n"), append = false)
                    newLines
                }
            }
        }
    }

    companion object {
        val FILE_PATH = "Log/logger.txt".toPath()
        private const val MAX_LINES = 1000
    }
}
