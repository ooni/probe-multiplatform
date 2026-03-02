package org.ooni.passport.models

data class PassportHttpResponse(
    val statusCode: Int,
    val version: String,
    val headersListText: List<List<String>>,
    val bodyText: String?,
) {
    val isSuccessful get() = statusCode in 200..299
}
