package org.ooni.probe.domain.appreview

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import org.ooni.probe.shared.today
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShouldShowAppReviewTest {
    @Test
    fun invoke() =
        runTest {
            assertTrue(
                ShouldShowAppReview(
                    incrementLaunchTimes = {},
                    getLaunchTimes = { 5 },
                    getShownAt = { null },
                    getFirstOpenAt = {
                        LocalDate.today().minus(DatePeriod(days = 3)).atTime(0, 0)
                    },
                    setFirstOpenAt = {},
                )(),
            )
            assertTrue(
                ShouldShowAppReview(
                    incrementLaunchTimes = {},
                    getLaunchTimes = { 10 },
                    getShownAt = { null },
                    getFirstOpenAt = {
                        LocalDate.today().minus(DatePeriod(days = 20)).atTime(0, 0)
                    },
                    setFirstOpenAt = {},
                )(),
            )
            assertFalse(
                ShouldShowAppReview(
                    incrementLaunchTimes = {},
                    getLaunchTimes = { 10 },
                    getShownAt = { null },
                    getFirstOpenAt = { null },
                    setFirstOpenAt = {},
                )(),
            )
            assertFalse(
                ShouldShowAppReview(
                    incrementLaunchTimes = {},
                    getLaunchTimes = { 3 },
                    getShownAt = { null },
                    getFirstOpenAt = {
                        LocalDate.today().minus(DatePeriod(days = 20)).atTime(0, 0)
                    },
                    setFirstOpenAt = {},
                )(),
            )
        }
}
