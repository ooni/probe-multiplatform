package org.ooni.probe.data.disk

import co.touchlab.kermit.Logger
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

fun interface ReadFile {
    suspend operator fun invoke(path: Path): String?
}

class ReadFileOkio(
    private val fileSystem: FileSystem,
    private val baseFilesDir: String,
) : ReadFile {
    override suspend fun invoke(path: Path): String? =
        try {
            fileSystem.read(baseFilesDir.toPath().resolve(path)) {
                readUtf8()
            }
        } catch (e: IOException) {
            Logger.v("Could not read $path", e)
            null
        }
}
