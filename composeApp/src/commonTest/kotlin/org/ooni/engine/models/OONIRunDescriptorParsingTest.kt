package org.ooni.engine.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OONIRunDescriptorParsingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parseFullJson() {
        val descriptorJson = """
        {
          "oonirun_link_id": "link123",
          "name": "Test Run",
          "short_description": "Short desc",
          "description": "Long desc",
          "author": "OONI",
          "icon": "icon_id",
          "expiration_date": "2026-12-31T23:59:59Z",
          "date_created": "2026-01-01T00:00:00Z",
          "date_updated": "2026-01-02T00:00:00Z",
          "is_expired": false,
          "revision": 1,
          "nettests": [
            {
              "test_name": "web_connectivity",
              "inputs": ["https://example.com/"],
              "inputs_extra": [
                { "category_code": "HUMR" }
              ],
              "targets_name": "websites_list_prioritized",
              "is_background_run_enabled_default": true,
              "is_manual_run_enabled_default": true
            }
          ]
        }
        """.trimIndent()

        val parsed = json.decodeFromString<OONIRunDescriptor>(descriptorJson)

        assertEquals("link123", parsed.oonirunLinkId)
        assertEquals(1, parsed.netTests.size)
        val netTest = parsed.netTests[0]
        assertEquals("web_connectivity", netTest.name)
        assertEquals(listOf("https://example.com/"), netTest.inputs)
        assertNotNull(netTest.inputsExtra)
        assertEquals(1, netTest.inputsExtra.size)
        assertEquals("HUMR", netTest.inputsExtra.first()["category_code"])
        assertEquals("websites_list_prioritized", netTest.targetsName)
        assertTrue(netTest.isBackgroundRunEnabled)
        assertTrue(netTest.isManualRunEnabled)
    }

    @Test
    fun parseOptionalFieldsMissing() {
        val descriptorJson = """
        {
          "oonirun_link_id": "link123",
          "name": "Test Run",
          "expiration_date": "2026-12-31T23:59:59Z",
          "date_created": "2026-01-01T00:00:00Z",
          "date_updated": "2026-01-02T00:00:00Z",
          "is_expired": false,
          "revision": 1,
          "nettests": [
            {
              "test_name": "web_connectivity"
            }
          ]
        }
        """.trimIndent()

        val parsed = json.decodeFromString<OONIRunDescriptor>(descriptorJson)

        assertEquals("link123", parsed.oonirunLinkId)
        assertEquals(null, parsed.shortDescription)
        assertEquals(null, parsed.description)
        assertEquals(null, parsed.author)

        val netTest = parsed.netTests[0]
        assertEquals(null, netTest.inputs)
        assertEquals(null, netTest.inputsExtra)
        assertEquals(null, netTest.targetsName)
        // Defaults should be false
        assertEquals(false, netTest.isBackgroundRunEnabled)
        assertEquals(false, netTest.isManualRunEnabled)
    }
}
