package org.ooni.probe.data.disk

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

fun interface DeleteFiles {
    suspend operator fun invoke(path: Path)
}

class DeleteFilesOkio(
    private val fileSystem: FileSystem,
    private val baseFilesDir: String,
) : DeleteFiles {
    override suspend fun invoke(path: Path) {
        fileSystem.deleteRecursively(baseFilesDir.toPath().resolve(path))
    }
}
