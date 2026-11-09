package org.ooni.probe.shared.monitoring

import co.touchlab.kermit.Severity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class AppLoggerTest {
    private val files = mutableMapOf<Path, String>()
    private val deletedPaths = mutableListOf<Path>()

    @Test
    fun loadsExistingLogFileOnStart() =
        runTest {
            files[AppLogger.FILE_PATH] = "line 1\n\nline 2\n"
            val subject = buildSubject()

            startIn(subject)

            assertEquals(listOf("line 1", "line 2"), subject.lines())
        }

    @Test
    fun keepsLinesLoggedBeforeStartAfterFileLines() =
        runTest {
            files[AppLogger.FILE_PATH] = "from file"
            val subject = buildSubject()
            subject.log(Severity.Info, "early")

            startIn(subject)

            val lines = subject.lines()
            assertEquals(2, lines.size)
            assertEquals("from file", lines[0])
            assertTrue(lines[1].endsWith(" : INFO : early"))
        }

    @Test
    fun keepsLinesInOrderWithSeverity() =
        runTest {
            val subject = buildSubject()
            startIn(subject)

            subject.log(Severity.Info, "first")
            subject.log(Severity.Warn, "second")
            subject.log(Severity.Debug, "third")
            runCurrent()

            val lines = subject.lines()
            assertEquals(3, lines.size)
            assertTrue(lines[0].endsWith(" : INFO : first"))
            assertTrue(lines[1].endsWith(" : WARN : second"))
            assertTrue(lines[2].endsWith(" : DEBUG : third"))
        }

    @Test
    fun publishesNewLinesOnlyAfterPublishInterval() =
        runTest {
            val subject = buildSubject()
            startIn(subject)

            subject.log(Severity.Info, "first batch")
            runCurrent()
            assertEquals(1, subject.lines().size)

            // Logged while the consumer waits for the next batch
            subject.log(Severity.Info, "second batch")
            advanceTimeBy(AppLogger.PUBLISH_INTERVAL - 100.milliseconds)
            runCurrent()
            assertEquals(1, subject.lines().size)

            advanceTimeBy(200.milliseconds)
            runCurrent()
            assertEquals(2, subject.lines().size)
        }

    @Test
    fun capsLogAtMaxLines() =
        runTest {
            val subject = buildSubject()
            startIn(subject)

            repeat(10_005) { subject.log(Severity.Info, "message $it") }
            runCurrent()

            val lines = subject.lines()
            assertEquals(10_000, lines.size)
            assertTrue(lines.first().endsWith(" : message 5"))
            assertTrue(lines.last().endsWith(" : message 10004"))
        }

    @Test
    fun readFiltersBySeverity() =
        runTest {
            val subject = buildSubject()
            startIn(subject)

            subject.log(Severity.Info, "info")
            subject.log(Severity.Warn, "warn")
            runCurrent()

            val warnings = subject.read(Severity.Warn).first()
            assertEquals(1, warnings.size)
            assertTrue(warnings[0].endsWith(" : WARN : warn"))
        }

    @Test
    fun verboseIsNotLoggable() =
        runTest {
            val subject = buildSubject()

            assertFalse(subject.logWriter.isLoggable("tag", Severity.Verbose))
            assertTrue(subject.logWriter.isLoggable("tag", Severity.Debug))
        }

    @Test
    fun writesLogToFilePeriodically() =
        runTest {
            val subject = buildSubject()
            startIn(subject)

            subject.log(Severity.Info, "persisted")
            runCurrent()
            assertNull(files[AppLogger.FILE_PATH])

            advanceTimeBy(AppLogger.WRITE_TO_FILE_INTERVAL)
            runCurrent()

            assertTrue(files[AppLogger.FILE_PATH].orEmpty().endsWith(" : INFO : persisted"))
        }

    @Test
    fun clearEmptiesLogAndDeletesFile() =
        runTest {
            val subject = buildSubject()
            startIn(subject)
            subject.log(Severity.Info, "to be cleared")
            runCurrent()

            subject.clear()

            assertEquals(emptyList(), subject.lines())
            assertEquals(listOf(AppLogger.FILE_PATH), deletedPaths)
        }

    private fun TestScope.buildSubject() =
        AppLogger(
            readFile = { files[it] },
            writeFile = { path, contents -> files[path] = contents },
            appendFile = { path, contents -> files[path] = files[path].orEmpty() + contents },
            deleteFiles = {
                deletedPaths += it
                files.remove(it)
            },
            backgroundContext = StandardTestDispatcher(testScheduler),
        )

    private fun TestScope.startIn(subject: AppLogger) {
        backgroundScope.launch { subject.start() }
        runCurrent()
    }

    private fun AppLogger.log(
        severity: Severity,
        message: String,
    ) = logWriter.log(severity, message, "tag", null)

    private suspend fun AppLogger.lines() = read(null).first()
}
