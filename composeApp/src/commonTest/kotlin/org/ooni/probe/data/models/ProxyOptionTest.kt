package org.ooni.probe.data.models

import kotlin.test.Test
import kotlin.test.assertEquals

class ProxyOptionTest {
    @Test
    fun buildCustomHttp() {
        val option = ProxyOption.Custom.build(
            "http",
            "example.org",
            "80",
        )
        assertEquals("http://example.org:80/", option.value)
    }

    @Test
    fun buildCustomHttpIPv6() {
        val option = ProxyOption.Custom.build(
            "https",
            "2001:db8:85a3:8d3:1319:8a2e:370:7348",
            "1234",
        )
        assertEquals("https://[2001:db8:85a3:8d3:1319:8a2e:370:7348]:1234/", option.value)
    }

    @Test
    fun buildInvalidProtocol() {
        val option = ProxyOption.Custom.build(
            "ooni",
            "example.org",
            "80",
        )
        assertEquals("http://example.org:80/", option.value)
    }
}
