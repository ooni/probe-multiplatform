package org.ooni.passport.models

import co.touchlab.kermit.Logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.ooni.probe.data.models.Credential

data class CredentialResponse(
    val response: PassportHttpResponse,
    val credential: String?,
) {
    fun decodeCredential(json: Json): Credential? =
        response.bodyText?.let {
            try {
                json.decodeFromString<Credential>(it)
            } catch (e: Exception) {
                Logger.w("Failed to decode credential response", e)
                null
            }
        }

    fun decodeSubmitOutcome(json: Json): SubmitOutcome {
        val body = response.bodyText
        if (body.isNullOrBlank()) return SubmitOutcome.EMPTY
        return try {
            val envelope = json.decodeFromString<Envelope>(body)
            SubmitOutcome(
                verificationStatus = VerificationStatus.fromWire(envelope.verificationStatus),
                error = SubmitError.fromWire(envelope.error),
            )
        } catch (e: Exception) {
            Logger.w("Failed to decode submit outcome", e)
            SubmitOutcome.EMPTY
        }
    }

    @Serializable
    private data class Envelope(
        @SerialName("verification_status") val verificationStatus: String? = null,
        @SerialName("error") val error: String? = null,
    )
}

data class SubmitOutcome(
    val verificationStatus: VerificationStatus,
    val error: SubmitError?,
) {
    companion object {
        val EMPTY = SubmitOutcome(VerificationStatus.Unknown, null)
    }
}
