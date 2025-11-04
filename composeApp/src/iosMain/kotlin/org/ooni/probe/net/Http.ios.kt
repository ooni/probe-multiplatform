package org.ooni.probe.net

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.probe.data.models.GetBytesException
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithURL
import platform.posix.memcpy
import kotlin.coroutines.resume

actual suspend fun httpGetBytes(url: String): Result<ByteArray, GetBytesException> =
    suspendCancellableCoroutine { cont ->
        val nsurl = NSURL.URLWithString(url) ?: run {
            cont.resume(Failure(GetBytesException(RuntimeException("Invalid URL: $url"))))
            return@suspendCancellableCoroutine
        }
        val task = NSURLSession.sharedSession.dataTaskWithURL(nsurl) { data, response, error ->
            if (error != null) {
                cont.resume(Failure(GetBytesException(RuntimeException(error.toString()))))
                return@dataTaskWithURL
            }

            when (response) {
                is platform.Foundation.NSHTTPURLResponse -> {
                    val statusCode = response.statusCode
                    if (statusCode in 200..299) {
                        cont.resume(Success(data?.toByteArray() ?: ByteArray(0)))
                    } else {
                        cont.resume(Failure(GetBytesException(RuntimeException("HTTP $statusCode while GET $url"))))
                    }
                }
                else -> {
                    // This could be for non-HTTP responses (e.g. file://) or an invalid state
                    if (data != null) {
                        cont.resume(Success(data.toByteArray()))
                    } else {
                        cont.resume(Failure(GetBytesException(RuntimeException("Request to $url returned no data and no error"))))
                    }
                }
            }
        }
        cont.invokeOnCancellation { task.cancel() }
        task.resume()
    }

private fun NSData.toByteArray(): ByteArray =
    ByteArray(length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), bytes, length)
        }
    }
