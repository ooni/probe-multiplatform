package org.ooni.probe.domain

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckSkipAutoRunNotUploadedLimitTest {
    @Test
    fun invoke() =
        runTest {
            suspend fun assertCheck(
                expected: Boolean,
                count: Long,
            ) {
                assertEquals(
                    expected,
                    CheckSkipAutoRunNotUploadedLimit(
                        countResultsMissingUpload = { flowOf(count) },
                    )(),
                )
            }

            assertCheck(expected = true, count = 20)
            assertCheck(expected = false, count = 5)
        }
}
