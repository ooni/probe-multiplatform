package org.ooni.probe.data.models

import org.ooni.engine.models.OONINetTest
import org.ooni.engine.models.TestType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetTestTest {
    @Test
    fun defaultsFromTestType() {
        val webConn = NetTest(test = TestType.WebConnectivity)
        assertTrue(webConn.isBackgroundRunEnabled)
        assertTrue(webConn.isManualRunEnabled)

        val dash = NetTest(test = TestType.Dash)
        assertFalse(dash.isBackgroundRunEnabled)
        assertTrue(dash.isManualRunEnabled)
    }

    @Test
    fun fromOONIStandardTestWithDescriptorDefaults() {
        // Bundled assets put false for both fields, but TestType defaults drive manual/background enablement
        val ooni = OONINetTest(
            name = "web_connectivity",
            inputs = listOf("https://example.com"),
            isBackgroundRunEnabled = false,
            isManualRunEnabled = false,
        )
        val netTest = NetTest.fromOONI(ooni)
        assertEquals(TestType.WebConnectivity, netTest.test)
        assertTrue(netTest.isBackgroundRunEnabled)
        assertTrue(netTest.isManualRunEnabled)
    }

    @Test
    fun fromOONIDashTestWithDescriptorDefaults() {
        val ooni = OONINetTest(
            name = "dash",
            isBackgroundRunEnabled = false,
            isManualRunEnabled = false,
        )
        val netTest = NetTest.fromOONI(ooni)
        assertEquals(TestType.Dash, netTest.test)
        assertFalse(netTest.isBackgroundRunEnabled)
        assertTrue(netTest.isManualRunEnabled)
    }

    @Test
    fun fromOONIExperimentalTestWithDescriptorDefaults() {
        val ooni = OONINetTest(
            name = "stunreachability",
            isBackgroundRunEnabled = false,
            isManualRunEnabled = false,
        )
        val netTest = NetTest.fromOONI(ooni)
        val experimentalTest = netTest.test as TestType.Experimental
        assertEquals("stunreachability", experimentalTest.name)
        assertFalse(experimentalTest.isBackgroundRunEnabled)
        assertTrue(experimentalTest.isManualRunEnabled)

        assertFalse(netTest.isBackgroundRunEnabled)
        assertTrue(netTest.isManualRunEnabled)
    }

    @Test
    fun fromOONIExperimentalTestPreservesExplicitFlagsOnTestType() {
        val ooni = OONINetTest(
            name = "custom_exp_test",
            inputs = emptyList(),
            isBackgroundRunEnabled = true,
            isManualRunEnabled = true,
        )
        val netTest = NetTest.fromOONI(ooni)
        assertTrue(netTest.isBackgroundRunEnabled)
        assertTrue(netTest.isManualRunEnabled)

        val experimentalTest = netTest.test as TestType.Experimental
        assertEquals("custom_exp_test", experimentalTest.name)
        assertTrue(experimentalTest.isBackgroundRunEnabled)
        assertTrue(experimentalTest.isManualRunEnabled)
    }

    @Test
    fun toOONIConversion() {
        val netTest = NetTest(
            test = TestType.Experimental(
                name = "exp_test",
                isBackgroundRunEnabled = true,
                isManualRunEnabled = true,
            ),
            inputs = listOf("https://ooni.org"),
            isBackgroundRunEnabled = true,
            isManualRunEnabled = true,
        )
        val ooni = netTest.toOONI()
        assertEquals("exp_test", ooni.name)
        assertEquals(listOf("https://ooni.org"), ooni.inputs)
        assertTrue(ooni.isBackgroundRunEnabled)
        assertTrue(ooni.isManualRunEnabled)
    }
}
