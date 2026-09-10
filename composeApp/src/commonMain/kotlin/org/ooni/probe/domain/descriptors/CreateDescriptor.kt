package org.ooni.probe.domain.descriptors

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.ooni.engine.models.Failure
import org.ooni.engine.models.OONIRunDescriptor
import org.ooni.engine.models.OONIRunLinkCreateRequest
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.engine.models.toModel
import org.ooni.passport.PassportBridge
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.domain.auth.bearerAuthHeader

/**
 * Creates a new OONI Run v2 link (`POST /api/v2/oonirun/links`).
 */
class CreateDescriptor(
    private val passportPostWithAuth: suspend (
        url: String,
        payload: String,
        extraHeaders: List<PassportBridge.KeyValue>,
    ) -> Result<PassportHttpResponse, PassportException>,
    private val getStoredSession: suspend () -> AuthSession?,
    private val json: Json,
) {
    suspend operator fun invoke(request: OONIRunLinkCreateRequest): Result<Descriptor, OonirunApiError> {
        val session = getStoredSession() ?: return Failure(OonirunApiError.Unauthorized("Not logged in"))
        if (request.author != session.emailAddress) {
            return Failure(
                OonirunApiError.BadRequest(
                    "email_address must match the email address of the user who created the oonirun link",
                ),
            )
        }

        // Fields left unset (name_intl, icon, expiration_date, ...) must be omitted rather than
        // sent as explicit nulls - the server only applies its own defaults (e.g. expiration_date
        // defaulting to now+6 months) when a field is absent, not when it's present as null.
        val body = json.encodeToString(
            JsonElement.serializer(),
            json.encodeToJsonElement(OONIRunLinkCreateRequest.serializer(), request).withoutNulls(),
        )
        val response = passportPostWithAuth(
            "${OrganizationConfig.ooniApiBaseUrl}/api/v2/oonirun/links",
            body,
            bearerAuthHeader(session.sessionToken),
        )

        return when (response) {
            is Failure -> Failure(OonirunApiError.Network(response.reason))
            is Success -> {
                val httpResponse = response.value
                if (!httpResponse.isSuccessful) {
                    Failure(parseOonirunApiError(httpResponse.statusCode, httpResponse.bodyText, json))
                } else {
                    try {
                        val descriptor = json.decodeFromString<OONIRunDescriptor>(httpResponse.bodyText.orEmpty())
                        Success(descriptor.toModel())
                    } catch (e: SerializationException) {
                        Failure(OonirunApiError.Decode(e))
                    } catch (e: IllegalArgumentException) {
                        Failure(OonirunApiError.Decode(e))
                    }
                }
            }
        }
    }
}

/**
 * Drops null-valued object entries at every nesting level, so an unset optional field is omitted
 * from the encoded JSON instead of sent as an explicit `null`.
 */
private fun JsonElement.withoutNulls(): JsonElement =
    when (this) {
        is JsonObject ->
            JsonObject(
                entries
                    .filter { it.value !is JsonNull }
                    .associate { it.key to it.value.withoutNulls() },
            )
        is JsonArray -> JsonArray(map { it.withoutNulls() })
        else -> this
    }
