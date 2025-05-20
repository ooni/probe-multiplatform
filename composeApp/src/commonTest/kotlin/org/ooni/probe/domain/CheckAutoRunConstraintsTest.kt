package org.ooni.probe.domain

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.NetworkType
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.BatteryState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
                        getBatteryState = { BatteryState.Charging },
                        knownNetworkType = true,
                        knownBatteryState = true,
                        countResultsMissingUpload = { flowOf(count) },
                    )(),
                )
            }

            assertCheck(expected = false, count = CheckAutoRunConstraints.NOT_UPLOADED_LIMIT + 1L)
            assertCheck(expected = true, count = CheckAutoRunConstraints.NOT_UPLOADED_LIMIT - 1L)
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
                getBatteryState = { BatteryState.Charging },
                knownNetworkType = true,
                knownBatteryState = true,
                countResultsMissingUpload = { flowOf(0) },
            )

            assertFalse(subject())
        }

    @Test
    fun checkCharging() =
        runTest {
            suspend fun test(
                onlyWhileCharging: Boolean,
                batteryState: BatteryState,
                knownBatteryState: Boolean,
            ) = CheckAutoRunConstraints(
                getAutoRunSettings = {
                    flowOf(
                        AutoRunParameters.Enabled(
                            wifiOnly = false,
                            onlyWhileCharging = onlyWhileCharging,
                        ),
                    )
                },
                getNetworkType = { NetworkType.Wifi },
                getBatteryState = { batteryState },
                knownNetworkType = true,
                knownBatteryState = knownBatteryState,
                countResultsMissingUpload = { flowOf(0) },
            )()

            assertTrue(
                test(onlyWhileCharging = true, BatteryState.Charging, knownBatteryState = true),
            )
            assertFalse(
                test(onlyWhileCharging = true, BatteryState.NotCharging, knownBatteryState = true),
            )
            assertTrue(
                test(onlyWhileCharging = true, BatteryState.Unknown, knownBatteryState = false),
            )
        }
}
