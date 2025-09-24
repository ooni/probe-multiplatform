package org.ooni.probe.data.models

import kotlin.test.Test
import kotlin.test.assertEquals

class ProxyOptionTest {
    @Test
    fun buildCustom() {
        val option = ProxyOption.Custom.build(
            "http",
            "example.org",
            "80",
        )
        assertEquals("http://example.org:80/", option.value)
    }
}
