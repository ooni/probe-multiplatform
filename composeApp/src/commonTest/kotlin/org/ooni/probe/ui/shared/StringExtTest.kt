package org.ooni.probe.ui.shared

import org.ooni.probe.shared.decodeUrlFromBase64
import org.ooni.probe.shared.encodeUrlToBase64
import kotlin.test.Test
import kotlin.test.assertEquals

class StringExtTest {
    @Test
    fun base64Encoding() {
        listOf(
            null,
            "http://example.org",
            "https://ooni.io",
            "https://dartcenter.org",
        ).forEach {
            assertEquals(it, it.encodeUrlToBase64().decodeUrlFromBase64())
        }
    }
}
