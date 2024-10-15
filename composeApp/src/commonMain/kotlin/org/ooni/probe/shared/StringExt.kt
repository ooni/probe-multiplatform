package org.ooni.probe.shared

import kotlin.io.encoding.Base64

private val BASE64 by lazy {
    Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
}

fun String?.encodeUrlToBase64() = BASE64.encode(orEmpty().encodeToByteArray())

fun String?.decodeUrlFromBase64() = this?.ifEmpty { null }?.let { BASE64.decode(it).decodeToString() }
