package org.ooni.probe.domain

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import org.ooni.engine.models.Result
import org.ooni.probe.data.models.GetBytesException

/**
 * Downloads binary content to a target absolute path using the provided fetcher.
 * - Creates parent directories if needed
 * - Skips writing if the target already exists with the same size
 */
class DownloadFile(
    private val fileSystem: FileSystem,
    private val fetchBytes: suspend (url: String) -> Result<ByteArray, GetBytesException>,
) {
    suspend operator fun invoke(
        url: String,
        absoluteTargetPath: String,
    ): Result<Path, GetBytesException> {
        val target = absoluteTargetPath.toPath()
        target.parent?.let { parent ->
            if (fileSystem.metadataOrNull(parent) == null) fileSystem.createDirectories(parent)
        }
        return fetchBytes(url).map { bytes ->
            fileSystem.sink(target).buffer().use { sink -> sink.write(bytes) }
            target
        }
    }
}
