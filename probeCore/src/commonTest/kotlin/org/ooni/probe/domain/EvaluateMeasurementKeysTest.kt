package org.ooni.probe.domain

import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.TestKeys
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.MeasurementKeysResult
import kotlin.test.Test
import kotlin.test.assertEquals

class EvaluateMeasurementKeysTest {
    @Test
    fun telegram() =
        runTest {
            assertEquals(
                MeasurementKeysResult(
                    isFailed = false,
                    isAnomaly = false,
                ),
                evaluateMeasurementKeys(
                    TestType.Telegram,
                    TestKeys(
                        telegramWebStatus = "ok",
                        telegramTcpBlocking = false,
                        telegramHttpBlocking = false,
                    ),
                ),
            )
            assertEquals(
                MeasurementKeysResult(
                    isFailed = false,
                    isAnomaly = true,
                ),
                evaluateMeasurementKeys(
                    TestType.Telegram,
                    TestKeys(
                        telegramWebStatus = "blocked",
                        telegramTcpBlocking = false,
                        telegramHttpBlocking = false,
                    ),
                ),
            )
            assertEquals(
                MeasurementKeysResult(
                    isFailed = true,
                    isAnomaly = false,
                ),
                evaluateMeasurementKeys(
                    TestType.Telegram,
                    TestKeys(
                        telegramWebStatus = null,
                        telegramTcpBlocking = false,
                        telegramHttpBlocking = false,
                    ),
                ),
            )
        }

    @Test
    fun signal() =
        runTest {
            assertEquals(
                MeasurementKeysResult(
                    isFailed = true,
                    isAnomaly = false,
                ),
                evaluateMeasurementKeys(
                    TestType.Signal,
                    TestKeys(),
                ),
            )
            assertEquals(
                MeasurementKeysResult(
                    isFailed = false,
                    isAnomaly = true,
                ),
                evaluateMeasurementKeys(
                    TestType.Signal,
                    TestKeys(signalBackendStatus = "blocked"),
                ),
            )
            assertEquals(
                MeasurementKeysResult(
                    isFailed = false,
                    isAnomaly = false,
                ),
                evaluateMeasurementKeys(
                    TestType.Signal,
                    TestKeys(signalBackendStatus = "ok"),
                ),
            )
        }
}
