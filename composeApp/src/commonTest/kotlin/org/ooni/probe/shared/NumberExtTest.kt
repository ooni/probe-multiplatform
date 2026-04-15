package org.ooni.probe.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberExtTest {
    @Test
    fun formatDataUsage() {
        assertEquals("0", 0L.formatDataUsage())
        assertEquals("100 B", 100L.formatDataUsage())
        assertEquals("1 kB", 1_024L.formatDataUsage())
        assertEquals("1.10 kB", 1_124L.formatDataUsage())
        assertEquals("2 kB", 2_048L.formatDataUsage())
        assertEquals("1 MB", (1_024L * 1_024L).formatDataUsage())
    }

    @Test
    fun format() {
        assertEquals("1", 1.0.format(2))
        assertEquals("1", 1.00009.format(1))
        assertEquals("1.09", 1.09.format(2))
        assertEquals("1.10", 1.099999.format(2))
        assertEquals("1.10", 1.1.format(2))
        assertEquals("1.90", 1.9.format(2))
        assertEquals("1.00009", 1.00009.format(5))
    }

    @Test
    fun largeNumberShort() {
        assertEquals("0", 0.largeNumberShort())
        assertEquals("100", 100.largeNumberShort())
        assertEquals("1K", 1_000.largeNumberShort())
        assertEquals("1M", 1_000_000.largeNumberShort())
        assertEquals("1.11M", 1_111_111.largeNumberShort())
        assertEquals("11.1M", 11_111_111.largeNumberShort())
    }
}
