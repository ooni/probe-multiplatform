package org.ooni.probe.domain

import kotlinx.coroutines.flow.first
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
                limit: Any?,
            ) {
                assertEquals(
                    expected,
                    CheckSkipAutoRunNotUploadedLimit(
                        countResultsMissingUpload = { flowOf(count) },
                        getPreferenceValueByKey = { flowOf(limit) },
                    )().first(),
                )
            }

            assertCheck(expected = true, count = 20, limit = null)
            assertCheck(expected = true, count = 5, limit = 5)
            assertCheck(expected = false, count = 0, limit = null)
            assertCheck(expected = false, count = 100, limit = 101)
        }
}
