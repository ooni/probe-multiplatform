package org.ooni.probe.domain

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/**
 * Downloads binary content to a target absolute path using the provided fetcher.
 * - Creates parent directories if needed
 * - Skips writing if the target already exists with the same size
 */
class DownloadFile(
    private val fileSystem: FileSystem,
    private val fetchBytes: suspend (url: String) -> ByteArray,
) {
    suspend operator fun invoke(
        url: String,
        absoluteTargetPath: String,
    ): Path {
        val target = absoluteTargetPath.toPath()
        target.parent?.let { parent ->
            if (fileSystem.metadataOrNull(parent) == null) fileSystem.createDirectories(parent)
        }
        val bytes = fetchBytes(url)
        val existing = fileSystem.metadataOrNull(target)
        if (existing?.size == bytes.size.toLong()) return target
        fileSystem.sink(target).buffer().use { sink -> sink.write(bytes) }
        return target
    }
}
