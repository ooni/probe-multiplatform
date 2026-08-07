package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import org.ooni.passport.models.SubmitError
import org.ooni.passport.models.VerificationStatus
import org.ooni.probe.shared.monitoring.Instrumentation
import org.ooni.probe.shared.monitoring.reportTransaction
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class HandleSubmitOutcome(
    private val retrieveManifest: suspend () -> Unit,
    private val clearCredential: ClearCredential,
    private val signalUpdateRequired: () -> Unit,
    private val now: () -> Instant = Clock.System::now,
) {
    private var lastManifestRefetchAt: Instant? = null

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
            SubmitError.ManifestNotFound -> refetchManifestThrottled()
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

    private suspend fun refetchManifestThrottled() {
        val last = lastManifestRefetchAt
        if (last != null && now() - last < MANIFEST_REFETCH_COOLDOWN) return
        lastManifestRefetchAt = now()
        retrieveManifest()
    }

    companion object {
        private val MANIFEST_REFETCH_COOLDOWN: Duration = 10.minutes
    }
}
