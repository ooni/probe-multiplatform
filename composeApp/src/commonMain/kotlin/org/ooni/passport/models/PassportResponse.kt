package org.ooni.passport.models

data class PassportResponse<T>(
    val body: T?,
    val httpResponse: PassportHttpResponse,
)
