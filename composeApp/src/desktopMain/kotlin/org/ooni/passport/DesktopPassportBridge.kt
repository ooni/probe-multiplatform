package org.ooni.passport

import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.SubmitCredentialConfig
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import uniffi.ooniprobe.CredentialResult
import uniffi.ooniprobe.HttpResponse
import uniffi.ooniprobe.KeyValue
import uniffi.ooniprobe.OoniException
import uniffi.ooniprobe.clientGet
import uniffi.ooniprobe.clientPost
import uniffi.ooniprobe.userauthRegister
import uniffi.ooniprobe.userauthSubmit

class DesktopPassportBridge : PassportBridge {
    override fun get(
        url: String,
        headers: List<PassportBridge.KeyValue>,
        query: List<PassportBridge.KeyValue>,
    ): Result<PassportHttpResponse, PassportException> =
        try {
            val response = clientGet(
                url = url,
                headers = headers.map { KeyValue(it.key, it.value) },
                query = query.map { KeyValue(it.key, it.value) },
            )
            Success(response.toPassport())
        } catch (e: OoniException) {
            Failure(e.toPassport())
        }

    override fun post(
        url: String,
        headers: List<PassportBridge.KeyValue>,
        payload: String,
    ): Result<PassportHttpResponse, PassportException> =
        try {
            val response = clientPost(
                url = url,
                headers = headers.map { KeyValue(it.key, it.value) },
                payload = payload,
            )
            Success(response.toPassport())
        } catch (e: OoniException) {
            Failure(e.toPassport())
        }

    override fun userAuthRegister(
        url: String,
        publicParams: String,
        manifestVersion: String,
    ): Result<CredentialResponse, PassportException> =
        try {
            val result = userauthRegister(url, publicParams, manifestVersion)
            Success(result.toPassport())
        } catch (e: OoniException) {
            Failure(e.toPassport())
        }

    override fun userAuthSubmit(
        url: String,
        content: String,
        probeCc: String,
        probeAsn: String,
        credentialConfig: SubmitCredentialConfig?,
    ): Result<CredentialResponse, PassportException> =
        try {
            val result = userauthSubmit(
                url = url,
                content = content,
                probeCc = probeCc,
                probeAsn = probeAsn,
                credentialConfig = credentialConfig?.toUniffi(),
            )
            Success(result.toPassport())
        } catch (e: OoniException) {
            Failure(e.toPassport())
        }

    override fun getProbeId(
        credentialB64: String,
        probeAsn: String,
        probeCc: String,
    ): Result<String, PassportException> =
        try {
            val result = uniffi.ooniprobe.getProbeId(credentialB64, probeAsn, probeCc)
            Success(result.probeId)
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

private fun CredentialResult.toPassport() =
    CredentialResponse(
        response = response.toPassport(),
        credential = credential,
    )

private fun SubmitCredentialConfig.toUniffi() =
    uniffi.ooniprobe.CredentialConfig(
        credential = credential,
        publicParams = publicParams,
        manifestVersion = manifestVersion,
        ageRange = uniffi.ooniprobe.ParamRange(min = ageRange.min, max = ageRange.max),
        measurementCountRange = uniffi.ooniprobe.ParamRange(
            min = measurementCountRange.min,
            max = measurementCountRange.max,
        ),
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
