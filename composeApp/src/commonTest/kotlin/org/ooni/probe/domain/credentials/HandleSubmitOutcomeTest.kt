package org.ooni.probe.domain.credentials

import kotlinx.coroutines.test.runTest
import org.ooni.engine.DeleteResult
import org.ooni.passport.models.SubmitError
import org.ooni.passport.models.VerificationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class HandleSubmitOutcomeTest {
    private var clearedKey: String? = null
    private var updateSignalled = false
    private var manifestRefetched = false

    private fun subject() =
        HandleSubmitOutcome(
            retrieveManifest = { manifestRefetched = true },
            clearCredential = ClearCredential(
                deleteSecureStorage = { key ->
                    clearedKey = key
                    DeleteResult.Deleted(key)
                },
            ),
            signalUpdateRequired = { updateSignalled = true },
        )

    private fun subjectWithClock(now: () -> Instant) =
        HandleSubmitOutcome(
            retrieveManifest = { manifestRefetched = true },
            clearCredential = ClearCredential(
                deleteSecureStorage = { key ->
                    clearedKey = key
                    DeleteResult.Deleted(key)
                },
            ),
            signalUpdateRequired = { updateSignalled = true },
            now = now,
        )

    @Test
    fun signalsUpdateForProtocolVersionTooOld() =
        runTest {
            subject()(VerificationStatus.Failed, SubmitError.ProtocolVersionTooOld)

            assertTrue(updateSignalled)
            assertFalse(manifestRefetched)
        }

    @Test
    fun signalsUpdateForProtocolError() =
        runTest {
            subject()(VerificationStatus.Failed, SubmitError.ProtocolError)
            assertTrue(updateSignalled)
        }

    @Test
    fun signalsUpdateForDeserializationFailed() =
        runTest {
            subject()(VerificationStatus.Failed, SubmitError.DeserializationFailed)
            assertTrue(updateSignalled)
        }

    @Test
    fun clearsCredentialForCredentialError() =
        runTest {
            subject()(VerificationStatus.Failed, SubmitError.CredentialError)

            assertEquals(CredentialsConstants.STORAGE_KEY, clearedKey)
            assertFalse(updateSignalled)
        }

    @Test
    fun refetchesManifestForManifestNotFound() =
        runTest {
            subject()(VerificationStatus.Unverified, SubmitError.ManifestNotFound)

            assertTrue(manifestRefetched)
            assertFalse(updateSignalled)
        }

    @Test
    fun doesNothingForVerifiedWithoutError() =
        runTest {
            subject()(VerificationStatus.Verified, null)

            assertFalse(updateSignalled)
            assertEquals(null, clearedKey)
            assertFalse(manifestRefetched)
        }

    @Test
    fun doesNothingForIncompleteAnoncFields() =
        runTest {
            subject()(VerificationStatus.Failed, SubmitError.IncompleteAnoncFields)

            assertFalse(updateSignalled)
            assertEquals(null, clearedKey)
            assertFalse(manifestRefetched)
        }

    @Test
    fun doesNothingForInvalidProtocolVersion() =
        runTest {
            subject()(VerificationStatus.Failed, SubmitError.InvalidProtocolVersion)

            assertFalse(updateSignalled)
            assertEquals(null, clearedKey)
            assertFalse(manifestRefetched)
        }

    @Test
    fun doesNothingForUnknownError() =
        runTest {
            subject()(VerificationStatus.Failed, SubmitError.Unknown("unexpected_error"))

            assertFalse(updateSignalled)
            assertEquals(null, clearedKey)
            assertFalse(manifestRefetched)
        }

    @Test
    fun doesNothingForUnverifiedWithoutError() =
        runTest {
            subject()(VerificationStatus.Unverified, null)

            assertFalse(updateSignalled)
            assertEquals(null, clearedKey)
            assertFalse(manifestRefetched)
        }

    @Test
    fun throttlesManifestRefetchWithinCooldown() =
        runTest {
            var currentTime = Instant.fromEpochSeconds(1_000_000)
            val outcome = subjectWithClock { currentTime }

            outcome(VerificationStatus.Unverified, SubmitError.ManifestNotFound)
            assertTrue(manifestRefetched)
            manifestRefetched = false

            currentTime += 5.minutes
            outcome(VerificationStatus.Unverified, SubmitError.ManifestNotFound)
            assertFalse(manifestRefetched)
        }

    @Test
    fun refetchesManifestAgainAfterCooldown() =
        runTest {
            var currentTime = Instant.fromEpochSeconds(1_000_000)
            val outcome = subjectWithClock { currentTime }

            outcome(VerificationStatus.Unverified, SubmitError.ManifestNotFound)
            assertTrue(manifestRefetched)
            manifestRefetched = false

            currentTime += 10.minutes
            outcome(VerificationStatus.Unverified, SubmitError.ManifestNotFound)
            assertTrue(manifestRefetched)
        }
}
