package org.ooni.probe.shared

import kotlin.io.encoding.Base64

fun String?.encodeUrlToBase64() = Base64.UrlSafe.encode(orEmpty().encodeToByteArray())

fun String?.decodeUrlFromBase64() = this?.ifEmpty { null }?.let { Base64.UrlSafe.decode(it).decodeToString() }
