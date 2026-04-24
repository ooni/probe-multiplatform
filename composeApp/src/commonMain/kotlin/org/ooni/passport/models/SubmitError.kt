package org.ooni.passport.models

sealed class SubmitError(
    val code: String,
) {
    object ManifestNotFound : SubmitError("manifest_not_found")

    object IncompleteAnoncFields : SubmitError("incomplete_anonc_fields")

    object ProtocolVersionTooOld : SubmitError("protocol_version_too_old")

    object InvalidProtocolVersion : SubmitError("invalid_protocol_version")

    object ProtocolError : SubmitError("protocol_error")

    object DeserializationFailed : SubmitError("deserialization_failed")

    object CredentialError : SubmitError("credential_error")

    data class Unknown(
        val raw: String,
    ) : SubmitError(raw)

    companion object {
        fun fromWire(value: String?): SubmitError? =
            when (value) {
                null, "" -> null
                "manifest_not_found" -> ManifestNotFound
                "incomplete_anonc_fields" -> IncompleteAnoncFields
                "protocol_version_too_old" -> ProtocolVersionTooOld
                "invalid_protocol_version" -> InvalidProtocolVersion
                "protocol_error" -> ProtocolError
                "deserialization_failed" -> DeserializationFailed
                "credential_error" -> CredentialError
                else -> Unknown(value)
            }
    }
}
