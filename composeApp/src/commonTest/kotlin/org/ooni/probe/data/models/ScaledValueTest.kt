package org.ooni.probe.data.models

import kotlin.test.Test
import kotlin.test.assertEquals

class ScaledValueTest {
    @Test
    fun test() {
        with(ScaledValue(0.0)) {
            assertEquals("0", scaledValue)
            assertEquals(ScaledValue.Unit.KB, unit)
        }
        with(ScaledValue(1.01)) {
            assertEquals("1.01", scaledValue)
            assertEquals(ScaledValue.Unit.KB, unit)
        }
        with(ScaledValue(999.0)) {
            assertEquals("999", scaledValue)
            assertEquals(ScaledValue.Unit.KB, unit)
        }
        with(ScaledValue(123_456.0)) {
            assertEquals("123.5", scaledValue)
            assertEquals(ScaledValue.Unit.MB, unit)
        }
        with(ScaledValue(123_456_789.0)) {
            assertEquals("123.5", scaledValue)
            assertEquals(ScaledValue.Unit.GB, unit)
        }
    }
}
