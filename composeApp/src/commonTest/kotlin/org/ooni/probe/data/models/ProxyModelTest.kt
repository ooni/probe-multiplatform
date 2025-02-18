package org.ooni.probe.data.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProxyModelTest {
    @Test
    fun newProxySettings_none() {
        val settings = ProxySettings.newProxySettings(
            protocol = null,
            hostname = null,
            port = null,
        )

        assertTrue(settings is ProxySettings.None)
        assertEquals("", settings.getProxyString())
    }

    @Test
    fun newProxySettings_psiphon() {
        val settings = ProxySettings.newProxySettings(
            protocol = "psiphon",
            hostname = null,
            port = null,
        )

        assertTrue(settings is ProxySettings.Psiphon)
        assertEquals("psiphon://", settings.getProxyString())
    }

    @Test
    fun newProxySettings_custom() {
        val settings = ProxySettings.newProxySettings(
            protocol = "http",
            hostname = "example.org",
            port = 80,
        )

        assertTrue(settings is ProxySettings.Custom)
        assertEquals(ProxyProtocol.HTTP, settings.protocol)
        assertEquals("example.org", settings.hostname)
        assertEquals(80, settings.port)
        assertEquals("http://example.org:80/", settings.getProxyString())
    }

    @Test
    fun newProxySettings_withInvalidPort() {
        val settings = ProxySettings.newProxySettings(
            protocol = "http",
            hostname = "example.org",
            port = null,
        )
        assertEquals(ProxySettings.None, settings)
    }
}
