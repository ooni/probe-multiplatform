package org.ooni.engine

import org.ooni.engine.models.NetworkType
import org.ooni.shared.DesktopBridgeLoader
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopNetworkTypeFinderTest {
    @Test
    fun loadsBundledDesktopBridgeFromClasspath() {
        // No compose.application.resources.dir and no java.library.path entry in tests:
        // the desktopbridge native library must come from the jar's classpath resources.
        assertTrue(DesktopBridgeLoader.ensureLoaded())

        val networkType = DesktopNetworkTypeFinder()()
        assertFalse(
            networkType is NetworkType.Unknown && networkType.value == "unknown",
            "Expected a real network type from the native bridge, got $networkType",
        )
    }
}
