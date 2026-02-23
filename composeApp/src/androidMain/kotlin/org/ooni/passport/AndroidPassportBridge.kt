package org.ooni.passport

import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.CredentialResult
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import uniffi.ooniprobe.HttpResponse
import uniffi.ooniprobe.KeyValue
import uniffi.ooniprobe.OoniException

class AndroidPassportBridge : PassportBridge {
    override suspend fun clientGet(
        url: String,
        headers: List<Map.Entry<String, String>>,
        query: List<Map.Entry<String, String>>,
    ): Result<PassportHttpResponse, PassportException> =
        try {
            val response = uniffi.ooniprobe.clientGet(
                url = url,
                headers = headers.map { KeyValue(it.key, it.value) },
                query = query.map { KeyValue(it.key, it.value) },
            )
            Success(response.toPassport())
        } catch (e: OoniException) {
            Failure(e.toPassport())
        }

    override suspend fun userAuthRegister(
        url: String,
        publicParams: String,
        manifestVersion: String,
    ): Result<CredentialResult, PassportException> =
        try {
            val result = uniffi.ooniprobe.userauthRegister(url, publicParams, manifestVersion)
            Success(result.toPassport())
        } catch (e: OoniException) {
            Failure(e.toPassport())
        }

    override suspend fun userAuthSubmit(
        url: String,
        credential: String,
        publicParams: String,
        content: String,
        probeCc: String,
        probeAsn: String,
        manifestVersion: String,
    ): Result<CredentialResult, PassportException> =
        try {
            val result = uniffi.ooniprobe.userauthSubmit(
                url = url,
                credential = credential,
                publicParams = publicParams,
                content = content,
                probeCc = probeCc,
                probeAsn = probeAsn,
                manifestVersion = manifestVersion,
            )
            Success(result.toPassport())
        } catch (e: OoniException) {
            Failure(e.toPassport())
        }
}

private fun HttpResponse.toPassport() =
    PassportHttpResponse(
        statusCode = statusCode,
        version = version,
        headersListText = headersListText,
        bodyText = bodyText,
    )

private fun uniffi.ooniprobe.CredentialResult.toPassport() =
    CredentialResult(
        response = response.toPassport(),
        credential = credential,
    )

private fun OoniException.toPassport() =
    when (this) {
        is OoniException.Base64DecodeException -> PassportException.Base64DecodeError(message)
        is OoniException.BinaryDecodeException -> PassportException.BinaryDecodeError(message)
        is OoniException.CryptoException -> PassportException.CryptoError(message)
        is OoniException.HttpClientException -> PassportException.HttpClientError(message)
        is OoniException.InvalidCredential -> PassportException.InvalidCredential(message)
        is OoniException.NullOrInvalidInput -> PassportException.NullOrInvalidInput(message)
        is OoniException.Other -> PassportException.Other(message)
        is OoniException.SerializationException -> PassportException.SerializationError(message)
    }
