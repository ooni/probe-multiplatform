package org.ooni.passport

import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.CredentialResult
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse

class IosPassportBridge(
    private val nativeBridge: NativePassportBridge,
) : PassportBridge {
    override suspend fun clientGet(
        url: String,
        headers: List<Map.Entry<String, String>>,
        query: List<Map.Entry<String, String>>,
    ): Result<PassportHttpResponse, PassportException> =
        when (
            val result =
                nativeBridge.nativeClientGet(
                    url = url,
                    headers = headers.map { KeyValuePair(it.key, it.value) },
                    query = query.map { KeyValuePair(it.key, it.value) },
                )
        ) {
            is NativeHttpResult.Success -> Success(result.response.toPassport())
            is NativeHttpResult.Error -> Failure(result.exception)
        }

    override suspend fun userAuthRegister(
        url: String,
        publicParams: String,
        manifestVersion: String,
    ): Result<CredentialResult, PassportException> =
        when (val result = nativeBridge.nativeUserAuthRegister(url, publicParams, manifestVersion)) {
            is NativeCredentialHttpResult.Success -> Success(result.result.toPassport())
            is NativeCredentialHttpResult.Error -> Failure(result.exception)
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
        when (
            val result =
                nativeBridge.nativeUserAuthSubmit(
                    url = url,
                    credential = credential,
                    publicParams = publicParams,
                    content = content,
                    probeCc = probeCc,
                    probeAsn = probeAsn,
                    manifestVersion = manifestVersion,
                )
        ) {
            is NativeCredentialHttpResult.Success -> Success(result.result.toPassport())
            is NativeCredentialHttpResult.Error -> Failure(result.exception)
        }
}

private fun NativeHttpResponse.toPassport() =
    PassportHttpResponse(
        statusCode = statusCode,
        version = version,
        headersListText = headersListText,
        bodyText = bodyText,
    )

private fun NativeCredentialResult.toPassport() =
    CredentialResult(
        response = response.toPassport(),
        credential = credential,
    )
