package org.ooni.probe.domain

import org.ooni.engine.models.NetworkType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoInternetWatcherTest {
    @Test
    fun internetBackResetsCounter() {
        var networkType: NetworkType = NetworkType.NoInternet
        val subject = NoInternetWatcher { networkType }

        repeat(NoInternetWatcher.NO_INTERNET_CANCEL_THRESHOLD - 1) {
            subject.checkIfShouldCancelDueToNoInternet()
        }
        networkType = NetworkType.Wifi
        assertFalse(subject.checkIfShouldCancelDueToNoInternet())
        assertEquals(0, subject.noInternetCounter)

        networkType = NetworkType.NoInternet
        repeat(NoInternetWatcher.NO_INTERNET_CANCEL_THRESHOLD - 1) {
            assertFalse(subject.checkIfShouldCancelDueToNoInternet())
        }
        assertTrue(subject.checkIfShouldCancelDueToNoInternet())
    }
}
