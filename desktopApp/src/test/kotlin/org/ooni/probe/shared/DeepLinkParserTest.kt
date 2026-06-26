package org.ooni.probe.shared

import org.junit.Test
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.DeepLink
import kotlin.test.assertEquals

class DeepLinkParserTest {
    @Test
    fun runUrls() {
        assertEquals(
            DeepLink.RunUrls("https://example.org"),
            DeepLinkParser("https://example.org"),
        )
    }

    @Test
    fun addDescriptorWithUrl() {
        assertEquals(
            DeepLink.AddDescriptor("10158"),
            DeepLinkParser("https://${OrganizationConfig.ooniRunDomain}/v2/10158"),
        )
    }

    @Test
    fun addDescriptorWithCustomScheme() {
        assertEquals(
            DeepLink.AddDescriptor("10158"),
            DeepLinkParser("ooni://runv2/10158"),
        )
    }

    @Test
    fun invalid() {
        assertEquals(
            DeepLink.Error,
            DeepLinkParser("invalid"),
        )
    }

    @Test
    fun invalidWithCustomScheme() {
        assertEquals(
            DeepLink.Error,
            DeepLinkParser("ooni://invalid"),
        )
    }

    @Test
    fun empty() {
        assertEquals(
            DeepLink.Error,
            DeepLinkParser(""),
        )
    }
}
