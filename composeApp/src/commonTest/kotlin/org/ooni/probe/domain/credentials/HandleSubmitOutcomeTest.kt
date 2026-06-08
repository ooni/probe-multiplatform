package org.ooni.probe.domain.credentials

import kotlinx.coroutines.test.runTest
import org.ooni.engine.DeleteResult
import org.ooni.passport.models.SubmitError
import org.ooni.passport.models.VerificationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
}
