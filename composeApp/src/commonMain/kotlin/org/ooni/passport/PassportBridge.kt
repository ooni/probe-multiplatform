package org.ooni.passport

import org.ooni.engine.models.Result
import org.ooni.passport.models.CredentialResult
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse

interface PassportBridge {
    suspend fun clientGet(
        url: String,
        headers: List<Map.Entry<String, String>> = emptyList(),
        query: List<Map.Entry<String, String>> = emptyList(),
    ): Result<PassportHttpResponse, PassportException>

    suspend fun userAuthRegister(
        url: String,
        publicParams: String,
        manifestVersion: String,
    ): Result<CredentialResult, PassportException>

    suspend fun userAuthSubmit(
        url: String,
        credential: String,
        publicParams: String,
        content: String,
        probeCc: String,
        probeAsn: String,
        manifestVersion: String,
    ): Result<CredentialResult, PassportException>
}
