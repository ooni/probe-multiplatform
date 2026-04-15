package org.ooni.probe.data.disk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FilesTest {
    private val fileSystem = FileSystem.SYSTEM
    private val baseFilesDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("ooni").toString()
    private val readFile = ReadFileOkio(fileSystem, baseFilesDir)
    private val writeFile = WriteFileOkio(fileSystem, baseFilesDir)
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
            writeFile(path, "hello", append = false)
            assertEquals("hello", readFile(path))
            writeFile(path, "hello", append = false)
            assertEquals("hello", readFile(path))
            writeFile(path, " world", append = true)
            assertEquals("hello world", readFile(path))
        }

    @Test
    fun readNonExistent() =
        runTest {
            assertEquals(null, readFile("test.txt".toPath()))
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
            writeFile(path, "hello", append = false)
            deleteFiles(path)
            assertEquals(null, readFile(path))
        }
}
