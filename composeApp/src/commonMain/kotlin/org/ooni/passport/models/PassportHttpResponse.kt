package org.ooni.passport.models

data class PassportHttpResponse(
    val statusCode: Int,
    val version: String,
    val headersListText: List<List<String>>,
    val bodyText: String?,
)
