package org.ooni.probe.data.disk

import co.touchlab.kermit.Logger
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

fun interface AppendFile {
    suspend operator fun invoke(
        path: Path,
        contents: String,
    )
}

class AppendFileOkio(
    private val fileSystem: FileSystem,
    private val baseFileDir: String,
) : AppendFile {
    override suspend fun invoke(
        path: Path,
        contents: String,
    ) {
        val absolutePath = baseFileDir.toPath().resolve(path)

        try {
            absolutePath.parent?.let { fileSystem.createDirectories(it) }
        } catch (e: IOException) {
            Logger.v("Could not create file $path", e)
            return
        }

        try {
            fileSystem.appendingSink(absolutePath).use { sink ->
                sink.buffer().use { it.writeUtf8(contents) }
            }
        } catch (e: Exception) {
            Logger.e("Could not append to file $path", e)
        }
    }
}
