package org.ooni.probe.domain

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.NetworkType
import org.ooni.probe.data.models.AutoRunParameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CheckAutoRunConstraintsTest {
    @Test
    fun checkResultsMissingUpload() =
        runTest {
            suspend fun assertCheck(
                expected: Boolean,
                count: Long,
            ) {
                assertEquals(
                    expected,
                    CheckAutoRunConstraints(
                        getAutoRunSettings = {
                            flowOf(
                                AutoRunParameters.Enabled(
                                    wifiOnly = false,
                                    onlyWhileCharging = false,
                                ),
                            )
                        },
                        getNetworkType = { NetworkType.Wifi },
                        isBatteryCharging = { true },
                        countResultsMissingUpload = { flowOf(count) },
                    )(),
                )
            }

            assertCheck(expected = false, count = 20)
            assertCheck(expected = true, count = 5)
        }

    @Test
    fun skipAutoRunIfVpnIsEnabled() =
        runTest {
            val subject = CheckAutoRunConstraints(
                getAutoRunSettings = {
                    flowOf(
                        AutoRunParameters.Enabled(
                            wifiOnly = false,
                            onlyWhileCharging = false,
                        ),
                    )
                },
                getNetworkType = { NetworkType.VPN },
                isBatteryCharging = { true },
                countResultsMissingUpload = { flowOf(0) },
            )

            assertFalse(subject())
        }
}
