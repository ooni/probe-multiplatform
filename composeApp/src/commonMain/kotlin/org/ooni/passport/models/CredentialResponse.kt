package org.ooni.passport.models

data class CredentialResponse(
    val response: PassportHttpResponse,
    val credential: String?,
)
