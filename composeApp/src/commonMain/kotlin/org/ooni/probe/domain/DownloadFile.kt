package org.ooni.probe.domain

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
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
import org.ooni.passport.models.PassportException
import org.ooni.probe.data.models.GetBytesException
import kotlin.time.Duration.Companion.seconds

/**
 * Downloads binary content to a target absolute path using the provided fetcher.
 * - Creates parent directories if needed
 * - Skips writing if the target already exists with the same size
 */
class DownloadFile(
    private val fileSystem: FileSystem,
    private val isOnline: () -> Boolean,
    private val httpClientFactory: () -> HttpClient = ::defaultHttpClient,
) {
    suspend operator fun invoke(
        url: String,
        absoluteTargetPath: String,
    ): Result<Path, GetBytesException> =
        fetchBytes(url).map { bytes ->
            // Only touch the filesystem once there is something to write, so a failed or skipped
            // download leaves no empty directory tree behind.
            val target = absoluteTargetPath.toPath()
            target.parent?.let { parent ->
                if (fileSystem.metadataOrNull(parent) == null) fileSystem.createDirectories(parent)
            }
            fileSystem.sink(target).buffer().use { sink -> sink.write(bytes) }
            target
        }

    /**
     * Perform a simple HTTP GET and return the raw response body bytes.
     * Uses Ktor HttpClient for cross-platform HTTP operations.
     */
    suspend fun fetchBytes(url: String): Result<ByteArray, GetBytesException> {
        if (!isOnline()) {
            return Failure(
                GetBytesException(PassportException.Offline("No active network, skipped $url")),
            )
        }

        val client = httpClientFactory()

        return try {
            val response = client.get(url)
            val bytes = response.bodyAsBytes()

            if (response.status.isSuccess()) {
                Success(bytes)
            } else {
                Failure(
                    GetBytesException(
                        Exception("HTTP ${response.status.value} while GET $url: ${bytes.decodeToString()}"),
                    ),
                )
            }
        } catch (e: Throwable) {
            Failure(GetBytesException(e))
        } finally {
            client.close()
        }
    }
}

/**
 * Timeouts are generous on the request as a whole because a GeoIP database is several MB, but
 * tight on connect: a black-holed network fails at connect time, and that is the case that used
 * to hang.
 */
private fun defaultHttpClient() =
    HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 120.seconds.inWholeMilliseconds
            connectTimeoutMillis = 10.seconds.inWholeMilliseconds
            socketTimeoutMillis = 30.seconds.inWholeMilliseconds
        }
    }
