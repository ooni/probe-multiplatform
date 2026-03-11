package org.ooni.probe.data.disk

import co.touchlab.kermit.Logger
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import kotlin.coroutines.CoroutineContext

fun interface DeleteFiles {
    suspend operator fun invoke(path: Path)
}

class DeleteFilesOkio(
    private val fileSystem: FileSystem,
    private val baseFilesDir: String,
    private val backgroundContext: CoroutineContext,
) : DeleteFiles {
    override suspend fun invoke(path: Path) {
        withContext(backgroundContext) {
            try {
                fileSystem.deleteRecursively(baseFilesDir.toPath().resolve(path))
            } catch (e: IOException) {
                Logger.v("Could not delete files at $path", e)
            }
        }
    }
}
