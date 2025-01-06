package org.ooni.probe.data.models

import org.ooni.engine.models.TestGroup
import org.ooni.engine.models.TestType
import kotlin.test.Test
import kotlin.test.assertEquals

class TestGroupTest {
    @Test
    fun fromTests() {
        assertEquals(
            TestGroup.Unknown,
            TestGroup.fromTests(emptyList()),
        )
        assertEquals(
            TestGroup.Websites,
            TestGroup.fromTests(listOf(TestType.WebConnectivity)),
        )
        assertEquals(
            TestGroup.Unknown,
            TestGroup.fromTests(listOf(TestType.WebConnectivity, TestType.Ndt)),
        )
        assertEquals(
            TestGroup.InstantMessaging,
            TestGroup.fromTests(listOf(TestType.FacebookMessenger, TestType.Whatsapp)),
        )
    }
}
