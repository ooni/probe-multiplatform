package org.ooni.passport

import org.ooni.passport.models.PassportException

interface NativePassportBridge {
    fun nativeClientGet(
        url: String,
        headers: List<KeyValuePair>,
        query: List<KeyValuePair>,
    ): NativeHttpResult

    fun nativeUserAuthRegister(
        url: String,
        publicParams: String,
        manifestVersion: String,
    ): NativeCredentialHttpResult

    fun nativeUserAuthSubmit(
        url: String,
        credential: String,
        publicParams: String,
        content: String,
        probeCc: String,
        probeAsn: String,
        manifestVersion: String,
    ): NativeCredentialHttpResult
}

data class KeyValuePair(
    val key: String,
    val value: String,
)

data class NativeHttpResponse(
    val statusCode: Int,
    val version: String,
    val headersListText: List<List<String>>,
    val bodyText: String?,
)

data class NativeCredentialResult(
    val response: NativeHttpResponse,
    val credential: String?,
)

sealed class NativeHttpResult {
    data class Success(
        val response: NativeHttpResponse,
    ) : NativeHttpResult()

    data class Error(
        val exception: PassportException,
    ) : NativeHttpResult()
}

sealed class NativeCredentialHttpResult {
    data class Success(
        val result: NativeCredentialResult,
    ) : NativeCredentialHttpResult()

    data class Error(
        val exception: PassportException,
    ) : NativeCredentialHttpResult()
}
