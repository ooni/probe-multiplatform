package org.ooni.probe.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

actual suspend fun httpGetBytes(url: String): ByteArray =
    withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection)
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val bytes = stream?.let { BufferedInputStream(it).use { bis -> bis.readBytes() } } ?: ByteArray(0)
            if (code !in 200..299) throw RuntimeException("HTTP $code while GET $url: ${bytes.decodeToString()}")
            bytes
        } finally {
            connection.disconnect()
        }
    }
