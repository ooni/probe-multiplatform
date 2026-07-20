package org.ooni.probe.data.disk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FilesTest {
    private val fileSystem = FileSystem.SYSTEM
    private val baseFilesDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("ooni").toString()
    private val readFile = ReadFileOkio(fileSystem, baseFilesDir)
    private val writeFile = WriteFileOkio(fileSystem, baseFilesDir)
    private val appendFile = AppendFileOkio(fileSystem, baseFilesDir)
    private val deleteFiles = DeleteFilesOkio(fileSystem, baseFilesDir, Dispatchers.Default)

    @AfterTest
    fun tearDown() =
        runTest {
            deleteFiles("".toPath())
        }

    @Test
    fun writeAndRead() =
        runTest {
            val path = "test.txt".toPath()
            writeFile(path, "hello")
            assertEquals("hello", readFile(path))
            writeFile(path, "hello")
            assertEquals("hello", readFile(path))
        }

    @Test
    fun appendAndRead() =
        runTest {
            val path = "test.txt".toPath()
            writeFile(path, "hello")
            appendFile(path, " world")
            assertEquals("hello world", readFile(path))
        }

    @Test
    fun readNonExistent() =
        runTest {
            assertEquals(null, readFile("test.txt".toPath()))
        }

    @Test
    fun overwriteIsAtomicAndLeavesNoTempFile() =
        runTest {
            val path = "report.json".toPath()
            writeFile(path, "first")
            writeFile(path, "second")
            assertEquals("second", readFile(path))
            // The atomic (temp + rename) write must not leave its scratch file behind.
            assertNull(readFile("report.json.tmp".toPath()))
        }

    @Test
    fun fallbackWriteSucceedsWhenAtomicMoveFails() =
        runTest {
            val fs = FailingAtomicMoveFileSystem(fileSystem, baseFilesDir)
            val write = WriteFileOkio(fs, baseFilesDir)
            val read = ReadFileOkio(fs, baseFilesDir)
            val path = "fallback.json".toPath()

            write(path, "content")
            assertEquals("content", read(path))
            // The scratch temp file must be cleaned up even when atomicMove fails.
            assertNull(fs.findTempFile(path))
        }

    @Test
    fun deleteNonExistent() =
        runTest {
            deleteFiles("test.txt".toPath())
        }

    @Test
    fun delete() =
        runTest {
            val path = "test.txt".toPath()
            writeFile(path, "hello")
            deleteFiles(path)
            assertEquals(null, readFile(path))
        }

    /**
     * A [ForwardingFileSystem] that throws on [atomicMove] so the write path falls back to direct
     * sink writing. Lets us verify the fallback behaviour without depending on OS-level failures.
     */
    private class FailingAtomicMoveFileSystem(
        delegate: FileSystem,
        private val baseFilesDir: String,
    ) : ForwardingFileSystem(delegate) {
        override fun atomicMove(
            source: Path,
            target: Path,
        ): Unit = throw IOException("Simulated atomicMove failure")

        fun findTempFile(finalPath: Path): String? {
            val dir = baseFilesDir.toPath().resolve(finalPath).parent ?: return null
            return delegate.list(dir).firstOrNull { it.name.endsWith(".tmp") }?.name
        }
    }
}
