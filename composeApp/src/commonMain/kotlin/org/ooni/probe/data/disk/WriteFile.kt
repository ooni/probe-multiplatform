package org.ooni.probe.data.disk

import co.touchlab.kermit.Logger
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

interface WriteFile {
    suspend operator fun invoke(
        path: Path,
        contents: String,
        append: Boolean,
    )
}

class WriteFileOkio(
    private val fileSystem: FileSystem,
    private val baseFileDir: String,
) : WriteFile {
    override suspend fun invoke(
        path: Path,
        contents: String,
        append: Boolean,
    ) {
        val absolutePath = baseFileDir.toPath().resolve(path)

        try {
            absolutePath.parent?.let { fileSystem.createDirectories(it) }

        } catch (e: IOException) {
            Logger.v("Could not create file $path", e)
            return
        }

        try {
            fileSystem
                .run { if (append) appendingSink(absolutePath) else sink(absolutePath) }
                .use { sink ->
                    sink.buffer().use {
                        it.writeUtf8(contents)
                    }
                }
        } catch (e: Exception) {
            Logger.e("Could not update file $path", e)
        }
    }
}
