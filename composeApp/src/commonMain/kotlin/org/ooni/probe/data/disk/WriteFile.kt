package org.ooni.probe.data.disk

import co.touchlab.kermit.Logger
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import kotlin.uuid.Uuid

fun interface WriteFile {
    suspend operator fun invoke(
        path: Path,
        contents: String,
    )
}

class WriteFileOkio(
    private val fileSystem: FileSystem,
    private val baseFileDir: String,
) : WriteFile {
    private var atomicMoveUnsupportedLogged = false

    override suspend fun invoke(
        path: Path,
        contents: String,
    ) {
        val absolutePath = baseFileDir.toPath().resolve(path)
        val parent = absolutePath.parent

        try {
            parent?.let { fileSystem.createDirectories(it) }
        } catch (e: IOException) {
            Logger.v("Could not create file $path", e)
            return
        }

        try {
            // Atomic replace: write to a sibling temp file in the SAME directory, then rename it
            // onto the final path. A process killed mid-write can only leave a stale `.tmp`, never a
            // truncated `absolutePath`. (See the corrupt-measurement-report investigation.)
            val tmp = (parent ?: absolutePath).resolve("${absolutePath.name}.${Uuid.generateV4()}.tmp")

            // Defensive: delete any orphaned `.tmp` siblings left by prior killed writes.
            parent?.let { dir ->
                runCatching {
                    fileSystem.list(dir).forEach { sibling ->
                        if (sibling.name.endsWith(".tmp")) {
                            fileSystem.delete(sibling, mustExist = false)
                        }
                    }
                }.onFailure { Logger.e("Could not clean up stale temp files in $dir", it) }
            }

            try {
                fileSystem.write(tmp) { writeUtf8(contents) }
                fileSystem.atomicMove(tmp, absolutePath)
            } catch (e: IOException) {
                // Never regress to "cannot write at all" if a filesystem can't do an atomic move.
                // Log only once: on a filesystem that never supports it this would otherwise spam
                // on every write (reports and the ~5s log rewrite).
                if (!atomicMoveUnsupportedLogged) {
                    atomicMoveUnsupportedLogged = true
                    Logger.w("Atomic write unsupported; using direct write from now on", e)
                }
                runCatching { fileSystem.delete(tmp, mustExist = false) }
                    .onFailure { Logger.e("Could not clean up temporary file $tmp", it) }
                fileSystem.sink(absolutePath).use { sink ->
                    sink.buffer().use { it.writeUtf8(contents) }
                }
            }
        } catch (e: Exception) {
            Logger.e("Could not update file $path", e)
        }
    }
}
