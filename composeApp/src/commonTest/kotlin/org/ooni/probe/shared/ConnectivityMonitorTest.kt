package org.ooni.probe.shared

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.models.NetworkType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectivityMonitorTest {
    @Test
    fun onlyNoInternetCountsAsOffline() {
        assertFalse(monitorOf(NetworkType.NoInternet).isOnline())

        listOf(
            NetworkType.Wifi,
            NetworkType.Mobile,
            NetworkType.VPN,
            NetworkType.Ethernet,
            NetworkType.Bluetooth,
            NetworkType.Usb,
            NetworkType.Unknown("unknown"),
        ).forEach { type ->
            assertTrue(monitorOf(type).isOnline(), "$type should count as online")
        }
    }

    @Test
    fun observeEmitsInitialStateThenOnlyTransitions() =
        runTest {
            // Repeated NoInternet then repeated Wifi: a naive poll would emit true three times.
            val sequence = listOf(
                NetworkType.NoInternet,
                NetworkType.NoInternet,
                NetworkType.Wifi,
                NetworkType.Wifi,
                NetworkType.Wifi,
            )
            var index = 0
            val subject = ConnectivityMonitor {
                sequence[minOf(index++, sequence.lastIndex)]
            }

            val emissions = subject.observeIsOnline().take(2).toList()

            assertEquals(listOf(false, true), emissions)
        }

    @Test
    fun observeStartsWithTrueWhenAlreadyOnline() =
        runTest {
            val subject = monitorOf(NetworkType.Wifi)

            val emissions = subject.observeIsOnline().take(1).toList()

            assertEquals(listOf(true), emissions)
        }

    private fun monitorOf(type: NetworkType) = ConnectivityMonitor(NetworkTypeFinder { type })
}
