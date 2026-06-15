package org.ooni.probe.ui.shared

import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DateFormatsTest {
    @Test
    fun format() =
        runComposeUiTest {
            setContent {
                assertEquals("0s", 0.seconds.format(abbreviated = true))
                assertEquals("1s", 1.seconds.format(abbreviated = true))
                assertEquals("1m", 1.minutes.format(abbreviated = true))
                assertEquals("1m 1s", 61.seconds.format(abbreviated = true))
                assertEquals("1 minute 1 second", 61.seconds.format(abbreviated = false))
                assertEquals("2 minutes 2 seconds", 122.seconds.format(abbreviated = false))
                assertEquals("1 hour", 1.hours.format(abbreviated = false))
            }
        }
}
