package org.ooni.probe.net

/**
 * Perform a simple HTTP GET and return the raw response body bytes.
 * Implemented per-platform to ensure binary-safe downloads.
 */
expect suspend fun httpGetBytes(url: String): ByteArray
