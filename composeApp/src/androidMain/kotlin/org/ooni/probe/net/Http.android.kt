package org.ooni.probe.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.probe.data.models.GetBytesException
import java.net.HttpURLConnection
import java.net.URL

actual suspend fun httpGetBytes(url: String): Result<ByteArray, GetBytesException> =
    withContext(Dispatchers.IO) {
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: Throwable) {
            return@withContext Failure(GetBytesException(e))
        }

        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
            if (code !in 200..299) {
                Failure(GetBytesException(Exception("HTTP $code while GET $url: ${String(bytes)}")))
            } else {
                Success(bytes)
            }
        } catch (e: Throwable) {
            Failure(GetBytesException(e))
        } finally {
            connection.disconnect()
        }
    }
