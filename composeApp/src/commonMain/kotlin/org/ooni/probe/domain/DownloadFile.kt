package org.ooni.probe.domain

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.probe.data.models.GetBytesException

/**
 * Downloads binary content to a target absolute path using the provided fetcher.
 * - Creates parent directories if needed
 * - Skips writing if the target already exists with the same size
 */
class DownloadFile(
    private val fileSystem: FileSystem,
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


    /**
     * Perform a simple HTTP GET and return the raw response body bytes.
     * Uses Ktor HttpClient for cross-platform HTTP operations.
     */
    suspend fun fetchBytes(url: String): Result<ByteArray, GetBytesException> {
         val client: HttpClient = HttpClient()

        return try {
            val response = client.get(url)
            val bytes = response.bodyAsBytes()

            if (response.status.isSuccess()) {
                Success(bytes)
            } else {
                Failure(
                    GetBytesException(
                        Exception("HTTP ${response.status.value} while GET $url: ${bytes.decodeToString()}")
                    )
                )
            }
        } catch (e: Throwable) {
            Failure(GetBytesException(e))
        } finally {
            client.close()
        }
    }
}
