package org.ooni.engine.models

import kotlinx.serialization.json.JsonPrimitive
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.di.Dependencies
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OONINetTestTest {
    private val json = Dependencies.buildJson()

    @Test
    fun decodesPerNetTestOptions() {
        val payload = """
            {
                "test_name": "web_connectivity",
                "options": {"SomeExperimentOption": "value"},
                "is_background_run_enabled_default": true,
                "is_manual_run_enabled_default": false
            }
        """.trimIndent()

        val netTest = json.decodeFromString<OONINetTest>(payload)

        assertNull(netTest.inputs)
        assertEquals(JsonPrimitive("value"), netTest.options?.get("SomeExperimentOption"))
    }

    @Test
    fun roundTripsThroughDomainNetTest() {
        val ooniNetTest = OONINetTest(
            name = "web_connectivity",
            inputs = listOf("https://ooni.org"),
            options = null,
        )

        val domain = NetTest.fromOONI(ooniNetTest)
        val backToOoni = domain.toOONI()

        assertEquals(ooniNetTest, backToOoni)
    }

    /**
     * The backend serializes `revision` as a JSON string (e.g. "1"), while this app models it as
     * a Long. This only decodes correctly because [Dependencies.buildJson] sets `isLenient = true`
     * - this test locks that behavior down so a future Json config change can't silently break it.
     */
    @Test
    fun decodesStringRevisionAsLong() {
        val payload = """
            {
                "oonirun_link_id": "1234",
                "name": "Test",
                "short_description": "Test",
                "description": "Test",
                "author": "author@example.org",
                "nettests": [],
                "name_intl": null,
                "short_description_intl": null,
                "description_intl": null,
                "icon": null,
                "color": null,
                "expiration_date": "2030-01-01T00:00:00Z",
                "date_created": "2025-01-01T00:00:00Z",
                "date_updated": "2025-01-01T00:00:00Z",
                "is_expired": false,
                "revision": "2"
            }
        """.trimIndent()

        val descriptor = json.decodeFromString<OONIRunDescriptor>(payload)

        assertEquals(2L, descriptor.revision)
    }
}
