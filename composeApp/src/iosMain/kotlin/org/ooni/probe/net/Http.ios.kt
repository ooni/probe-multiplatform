package org.ooni.probe.net

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithURL
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun httpGetBytes(url: String): ByteArray =
    suspendCancellableCoroutine { cont ->
        val nsurl = NSURL.URLWithString(url)!!
        val task = NSURLSession.sharedSession.dataTaskWithURL(nsurl) { data, response, error ->
            when {
                error != null -> cont.resumeWithException(RuntimeException(error.localizedDescription))
                data != null -> {
                    // If we have an HTTP response, check status code
                    val http = response as? platform.Foundation.NSHTTPURLResponse
                    val status = http?.statusCode?.toInt() ?: 200
                    if (status in 200..299) {
                        cont.resume((data as NSData).toByteArray())
                    } else {
                        cont.resumeWithException(RuntimeException("HTTP $status while GET $url"))
                    }
                }
                else -> cont.resume(ByteArray(0))
            }
        }
        cont.invokeOnCancellation { task.cancel() }
        task.resume()
    }

private fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    result.usePinned {
        memcpy(it.addressOf(0), this.bytes, this.length)
    }
    return result
}
