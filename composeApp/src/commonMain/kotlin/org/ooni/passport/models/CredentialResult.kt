package org.ooni.passport.models

data class CredentialResult(
    val response: PassportHttpResponse,
    val credential: String?,
)
