package org.ooni.passport.models

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
                null
            }
        }
}
