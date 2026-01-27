package org.ooni.probe.domain.results

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.RunModel
import org.ooni.testing.factories.ResultModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GetLastRunTest {
    @Test
    fun nullWhenNoResults() =
        runTest {
            val subject = GetLastRun(
                getLastRunResults = { flowOf(emptyList()) },
                getLastResult = { flowOf(null) },
                getPreference = { _ -> flowOf(null) },
            )
            assertNull(subject().first())
        }

    @Test
    fun nullWhenResultIsDismissed() =
        runTest {
            val runId = RunModel.Id("12345")
            val result = ResultModelFactory.buildWithNetworkAndAggregates(
                result = ResultModelFactory.build(runId = runId),
            )

            val subject = GetLastRun(
                getLastRunResults = { flowOf(listOf(result)) },
                getLastResult = { flowOf(result) },
                getPreference = { _ -> flowOf(runId.value) },
            )

            assertNull(subject().first())
        }

    @Test
    fun returnsLastRunWhenNotDismissed() =
        runTest {
            val runId = RunModel.Id("12345")
            val result = ResultModelFactory.buildWithNetworkAndAggregates(
                result = ResultModelFactory.build(runId = runId),
            )

            val subject = GetLastRun(
                getLastRunResults = { flowOf(listOf(result)) },
                getLastResult = { flowOf(result) },
                getPreference = { _ -> flowOf(null) },
            )

            val run = subject().first()!!
            assertNotNull(run)
            assertEquals(runId, run.run.id)
            assertEquals(listOf(result), run.results)
        }
}
