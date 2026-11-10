package org.ooni.probe.domain.auth

import kotlinx.serialization.DeserializationStrategy
import org.ooni.engine.models.Result
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.di.Dependencies

fun <T> decodeAuthResponse(
    response: PassportHttpResponse,
    deserializer: DeserializationStrategy<T>,
): Result<T, AuthException> = DecodeAuthResponse(Dependencies.buildJson())(response, deserializer)
