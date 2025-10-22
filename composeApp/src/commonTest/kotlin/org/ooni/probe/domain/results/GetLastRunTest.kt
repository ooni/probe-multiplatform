package org.ooni.probe.domain.results

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.testing.factories.ResultModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetLastRunTest {
    @Test
    fun nullWhenNoResults() =
        runTest {
            val subject = GetLastRun(
                getLastResults = { _ -> flowOf(emptyList()) },
                getPreference = { _ -> flowOf(null) },
            )
            assertNull(subject().first())
        }

    @Test
    fun nullWhenResultIsDismissed() =
        runTest {
            val result1 = ResultModelFactory.buildWithNetworkAndAggregates(
                result = ResultModelFactory.build(descriptorName = "websites"),
            )

            val subject = GetLastRun(
                getLastResults = { _ -> flowOf(listOf(result1)) },
                getPreference = { _ -> flowOf(result1.result.id?.value) },
            )

            assertNull(subject().first())
        }

    @Test
    fun doesNotRepeatDescriptors() =
        runTest {
            val result1 = ResultModelFactory.buildWithNetworkAndAggregates(
                result = ResultModelFactory.build(descriptorName = "websites", isDone = true),
            )
            val result2 = ResultModelFactory.buildWithNetworkAndAggregates(
                result = ResultModelFactory.build(descriptorName = "websites", isDone = true),
            )

            val subject = GetLastRun(
                getLastResults = { _ -> flowOf(listOf(result1, result2)) },
                getPreference = { _ -> flowOf(null) },
            )

            val run = subject().first()!!
            assertEquals(1, run.results.size)
            assertTrue(run.results.contains(result1))
        }
}
