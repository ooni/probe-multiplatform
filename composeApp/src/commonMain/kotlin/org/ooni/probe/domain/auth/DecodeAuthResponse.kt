package org.ooni.probe.domain.auth

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.PassportHttpResponse

class DecodeAuthResponse(
    private val json: Json,
) {
    operator fun <T> invoke(
        response: PassportHttpResponse,
        deserializer: DeserializationStrategy<T>,
    ): Result<T, AuthException> {
        if (!response.isSuccessful) {
            return Failure(AuthException.Http(response.statusCode, response.bodyText))
        }
        val body = response.bodyText ?: return Failure(AuthException.Http(response.statusCode, null))
        return try {
            Success(json.decodeFromString(deserializer, body))
        } catch (e: SerializationException) {
            Failure(AuthException.Decode(e))
        } catch (e: IllegalArgumentException) {
            Failure(AuthException.Decode(e))
        }
    }
}
