package org.ooni.probe.ui.shared

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationsTest {
    @Test
    fun isValidUrl() {
        assertFalse("".isValidUrl())
        assertFalse("http://".isValidUrl())
        assertFalse("https://".isValidUrl())
        assertFalse("http://a".isValidUrl())
        assertFalse("http://a.".isValidUrl())
        assertTrue("http://example.org".isValidUrl())
        assertTrue("http://example.co.uk".isValidUrl())
        assertTrue("http://example.org/path".isValidUrl())
        assertTrue("http://example.org?query=something".isValidUrl())
    }
}
