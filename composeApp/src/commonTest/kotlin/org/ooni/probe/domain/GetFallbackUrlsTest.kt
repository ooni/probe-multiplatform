package org.ooni.probe.domain

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.BatteryState
import org.ooni.testing.factories.MeasurementModelFactory
import org.ooni.testing.factories.UrlModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class GetFallbackUrlsTest {
    @Test
    fun empty() =
        runTest {
            val subject = GetFallbackUrls(
                getMeasurementsWithUrl = { flowOf(emptyList()) },
                getBatteryState = { BatteryState.Charging },
                sampleSize = 10,
            )
            assertEquals(0, subject(TaskOrigin.OoniRun).size)
        }

    @Test
    fun one() =
        runTest {
            val measurement = MeasurementModelFactory.buildWithUrl()
            val subject = GetFallbackUrls(
                getMeasurementsWithUrl = { flowOf(listOf(measurement)) },
                getBatteryState = { BatteryState.Charging },
                sampleSize = 10,
            )
            val result = subject(TaskOrigin.OoniRun)
            assertEquals(1, result.size)
            assertEquals(measurement.url, result.first())
        }

    @Test
    fun sampleOne() =
        runTest {
            val measurements = (1..10).map { MeasurementModelFactory.buildWithUrl() }
            val subject = GetFallbackUrls(
                getMeasurementsWithUrl = { flowOf(measurements) },
                getBatteryState = { BatteryState.Charging },
                sampleSize = 1,
            )
            val result = subject(TaskOrigin.OoniRun)
            assertEquals(1, result.size)
        }

    @Test
    fun sameUrls() =
        runTest {
            val url = UrlModelFactory.build()
            val measurements = (1..10).map { MeasurementModelFactory.buildWithUrl(url = url) }
            val subject = GetFallbackUrls(
                getMeasurementsWithUrl = { flowOf(measurements) },
                getBatteryState = { BatteryState.Charging },
                sampleSize = 10,
            )
            val result = subject(TaskOrigin.OoniRun)
            assertEquals(1, result.size)
        }

    @Test
    fun allLocalUrls() =
        runTest {
            val measurements = (1..10).map {
                MeasurementModelFactory.buildWithUrl(url = UrlModelFactory.build(countryCode = "IT"))
            }
            val subject = GetFallbackUrls(
                getMeasurementsWithUrl = { flowOf(measurements) },
                getBatteryState = { BatteryState.Charging },
                sampleSize = 10,
            )
            val result = subject(TaskOrigin.OoniRun)
            assertEquals(0, result.size)
        }

    @Test
    fun sampleAllDifferent() =
        runTest {
            val measurements = (1..10).map { MeasurementModelFactory.buildWithUrl() }
            val subject = GetFallbackUrls(
                getMeasurementsWithUrl = { flowOf(measurements) },
                getBatteryState = { BatteryState.Charging },
                sampleSize = 10,
            )
            val result = subject(TaskOrigin.OoniRun)
            assertEquals(10, result.size)
        }
}
