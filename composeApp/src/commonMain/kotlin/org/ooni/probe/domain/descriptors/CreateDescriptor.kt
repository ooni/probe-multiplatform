package org.ooni.probe.domain.descriptors

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
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
    private val parseOonirunApiError: ParseOonirunApiError,
    /**
     * Configured with `explicitNulls = false`: fields left unset (name_intl, icon,
     * expiration_date, ...) must be omitted rather than sent as explicit nulls, since the server
     * only applies its own defaults (e.g. expiration_date defaulting to now+6 months) when a
     * field is absent, not when it's present as null.
     */
    private val requestJson: Json,
    private val responseJson: Json,
) {
    suspend operator fun invoke(request: OONIRunLinkCreateRequest): Result<Descriptor, OonirunApiError> {
        val session = getStoredSession() ?: return Failure(OonirunApiError.Unauthorized("Not logged in"))

        val body = requestJson.encodeToString(
            OONIRunLinkCreateRequest.serializer(),
            request.copy(author = session.emailAddress),
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
                    Failure(parseOonirunApiError(httpResponse.statusCode, httpResponse.bodyText))
                } else {
                    try {
                        val descriptor =
                            responseJson.decodeFromString<OONIRunDescriptor>(httpResponse.bodyText.orEmpty())
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
