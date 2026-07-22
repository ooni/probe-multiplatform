package org.ooni.passport.models

import org.ooni.engine.Engine.MkException
import org.ooni.probe.data.models.GetBytesException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineFailureTest {
    @Test
    fun offlineIsAnOfflineFailure() {
        assertTrue(PassportException.Offline("no active network").isOfflineFailure())
    }

    @Test
    fun offlineIsSeenThroughWrappers() {
        val offline = PassportException.Offline("no active network")

        assertTrue(MkException(offline).isOfflineFailure())
        assertTrue(GetBytesException(offline).isOfflineFailure())
        assertTrue(MkException(GetBytesException(offline)).isOfflineFailure())
    }

    /**
     * The submit path builds an `HttpClientError` for every non-2XX response. Classifying those as
     * offline would silence real server errors and make the descriptor worker retry requests the
     * server already rejected.
     */
    @Test
    fun submitHttpErrorsAreNotOfflineFailures() {
        listOf(400, 404, 500).forEach { status ->
            val exception = PassportException.HttpClientError("Submit returned HTTP $status")
            assertFalse(
                exception.isOfflineFailure(),
                "HTTP $status must not be classified as an offline failure",
            )
            assertFalse(MkException(exception).isOfflineFailure())
        }
    }

    @Test
    fun otherPassportFailuresAreNotOfflineFailures() {
        assertFalse(PassportException.SerializationError("bad json").isOfflineFailure())
        assertFalse(PassportException.CryptoError("bad key").isOfflineFailure())
        assertFalse(PassportException.InvalidCredential("expired").isOfflineFailure())
        assertFalse(PassportException.Other("boom").isOfflineFailure())
    }

    @Test
    fun nullAndUnrelatedThrowablesAreNotOfflineFailures() {
        assertFalse((null as Throwable?).isOfflineFailure())
        assertFalse(IllegalStateException("boom").isOfflineFailure())
    }

    @Test
    fun selfReferencingCauseChainTerminates() {
        val loop = SelfCausedException()
        assertFalse(loop.isOfflineFailure())
    }

    private class SelfCausedException : Exception() {
        override val cause: Throwable get() = this
    }
}
