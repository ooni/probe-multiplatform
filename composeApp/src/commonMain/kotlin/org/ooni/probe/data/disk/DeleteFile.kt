package org.ooni.probe.data.disk

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

interface DeleteFile {
    suspend operator fun invoke(path: Path)
}

class DeleteFileOkio(
    private val fileSystem: FileSystem,
    private val baseFilesDir: String,
) : DeleteFile {
    override suspend fun invoke(path: Path) {
        fileSystem.delete(baseFilesDir.toPath().resolve(path))
    }
}
