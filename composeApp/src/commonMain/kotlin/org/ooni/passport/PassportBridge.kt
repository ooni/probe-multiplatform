package org.ooni.passport

import org.ooni.engine.models.Result
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse

interface PassportBridge {
    fun get(
        url: String,
        headers: List<KeyValue> = emptyList(),
        query: List<KeyValue> = emptyList(),
    ): Result<PassportHttpResponse, PassportException>

    fun userAuthRegister(
        url: String,
        publicParams: String,
        manifestVersion: String,
    ): Result<CredentialResponse, PassportException>

    fun userAuthSubmit(
        url: String,
        credential: String,
        publicParams: String,
        content: String,
        probeCc: String,
        probeAsn: String,
        manifestVersion: String,
    ): Result<CredentialResponse, PassportException>

    data class KeyValue(
        val key: String,
        val value: String,
    )
}
