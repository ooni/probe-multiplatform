package org.ooni.probe.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetStorageUsedTest {
    private val fileSystem = FileSystem.SYSTEM
    private lateinit var root: Path
    private lateinit var baseDir: Path
    private lateinit var cacheDir: Path

    @BeforeTest
    fun setUp() {
        root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "GetStorageUsedTest-${Random.nextLong()}"
        baseDir = root / "files"
        cacheDir = root / "cache"
        fileSystem.createDirectories(baseDir)
        fileSystem.createDirectories(cacheDir)
    }

    @AfterTest
    fun tearDown() {
        fileSystem.deleteRecursively(root)
    }

    @Test
    fun emptyDirectories() =
        runTest {
            assertEquals(0L, subject().update())
        }

    @Test
    fun sumsRegularFilesInBothDirectoriesRecursively() =
        runTest {
            writeFile(baseDir / "a.txt", 10)
            writeFile(baseDir / "Measurement" / "1" / "report.json", 25)
            writeFile(cacheDir / "c.bin", 7)
            writeFile(cacheDir / "nested" / "deeper" / "d.bin", 3)
            // Empty directories add nothing
            fileSystem.createDirectories(baseDir / "empty")

            assertEquals(45L, subject().update())
        }

    @Test
    fun updateReflectsDeletedFiles() =
        runTest {
            writeFile(baseDir / "a.txt", 10)
            writeFile(cacheDir / "b.txt", 20)
            val subject = subject()
            assertEquals(30L, subject.update())

            fileSystem.delete(cacheDir / "b.txt")

            assertEquals(10L, subject.update())
        }

    @Test
    fun missingCacheDirIsIgnored() =
        runTest {
            writeFile(baseDir / "a.txt", 10)
            fileSystem.deleteRecursively(cacheDir)

            assertEquals(10L, subject().update())
        }

    @Test
    fun missingBaseDirIsIgnored() =
        runTest {
            writeFile(cacheDir / "c.txt", 7)
            fileSystem.deleteRecursively(baseDir)

            assertEquals(7L, subject().update())
        }

    @Test
    fun bothDirsMissingReportsZero() =
        runTest {
            fileSystem.deleteRecursively(root)

            assertEquals(0L, subject().update())
        }

    @Test
    fun observeComputesOnStartAndEmitsUpdates() =
        runTest {
            writeFile(baseDir / "a.txt", 10)
            val subject = subject()

            assertEquals(10L, subject.observe().first())

            writeFile(cacheDir / "b.txt", 5)
            subject.update()

            assertEquals(15L, subject.observe().first())
        }

    private fun writeFile(
        path: Path,
        size: Int,
    ) {
        path.parent?.let(fileSystem::createDirectories)
        fileSystem.write(path) { write(ByteArray(size)) }
    }

    private fun TestScope.subject() =
        GetStorageUsed(
            backgroundContext = coroutineContext,
            baseFileDir = baseDir.toString(),
            fileSystem = fileSystem,
            cacheDir = cacheDir.toString(),
        )
}
