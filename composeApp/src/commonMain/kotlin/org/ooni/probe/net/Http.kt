package org.ooni.probe.net

import org.ooni.engine.models.Result
import org.ooni.probe.data.models.GetBytesException

/**
 * Perform a simple HTTP GET and return the raw response body bytes.
 * Implemented per-platform to ensure binary-safe downloads.
 */
expect suspend fun httpGetBytes(url: String): Result<ByteArray, GetBytesException>
