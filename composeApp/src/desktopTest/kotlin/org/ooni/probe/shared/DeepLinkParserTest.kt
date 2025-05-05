package org.ooni.probe.shared

import org.junit.Test
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.DeepLink
import kotlin.test.assertEquals

class DeepLinkParserTest {
    @Test
    fun test() {
        assertEquals(
            DeepLink.RunUrls("https://example.org"),
            DeepLinkParser("https://example.org"),
        )
        assertEquals(
            DeepLink.AddDescriptor("10158"),
            DeepLinkParser("https://${OrganizationConfig.ooniRunDomain}/v2/10158"),
        )
        assertEquals(
            DeepLink.AddDescriptor("10158"),
            DeepLinkParser("ooni://runv2/10158"),
        )
        assertEquals(
            DeepLink.Error,
            DeepLinkParser("ooni://invalid"),
        )
        assertEquals(
            DeepLink.Error,
            DeepLinkParser("invalid"),
        )
    }
}
