package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import org.ooni.passport.models.SubmitError
import org.ooni.passport.models.VerificationStatus
import org.ooni.probe.shared.monitoring.Instrumentation
import org.ooni.probe.shared.monitoring.reportTransaction

class HandleSubmitOutcome(
    private val retrieveManifest: suspend () -> Unit,
    private val clearCredential: ClearCredential,
    private val signalUpdateRequired: () -> Unit,
) {
    suspend operator fun invoke(
        verificationStatus: VerificationStatus,
        error: SubmitError?,
    ) {
        if (error != null) {
            Logger.w("Submit outcome: status=$verificationStatus error=${error.code}")
            Instrumentation.reportTransaction(
                operation = "SubmitOutcomeError",
                data = mapOf(
                    "verification_status" to verificationStatus.name,
                    "error" to error.code,
                ),
            )
        }

        when (error) {
            SubmitError.ManifestNotFound -> retrieveManifest()
            SubmitError.IncompleteAnoncFields -> Unit
            SubmitError.InvalidProtocolVersion -> Unit
            SubmitError.ProtocolVersionTooOld,
            SubmitError.ProtocolError,
            SubmitError.DeserializationFailed,
            -> signalUpdateRequired()
            SubmitError.CredentialError -> clearCredential()
            is SubmitError.Unknown -> Unit
            null -> Unit
        }
    }
}
