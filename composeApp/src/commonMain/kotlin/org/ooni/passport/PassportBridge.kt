package org.ooni.passport

import org.ooni.engine.models.Result
import org.ooni.passport.PassportBridge.KeyValue
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.SubmitCredentialConfig
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse

interface PassportBridge :
    PassportGet,
    PassportAuthRegister,
    PassportAuthSubmit,
    PassportGetProbeId,
    PassportPost {
    override fun get(
        url: String,
        headers: List<KeyValue>,
        query: List<KeyValue>,
    ): Result<PassportHttpResponse, PassportException>

    override fun post(
        url: String,
        headers: List<KeyValue>,
        payload: String,
    ): Result<PassportHttpResponse, PassportException>

    override fun userAuthRegister(
        url: String,
        publicParams: String,
        manifestVersion: String,
    ): Result<CredentialResponse, PassportException>

    override fun userAuthSubmit(
        url: String,
        content: String,
        probeCc: String,
        probeAsn: String,
        credentialConfig: SubmitCredentialConfig?,
    ): Result<CredentialResponse, PassportException>

    override fun getProbeId(
        credentialB64: String,
        probeAsn: String,
        probeCc: String,
    ): Result<String, PassportException>

    data class KeyValue(
        val key: String,
        val value: String,
    )
}

fun interface PassportGet {
    fun get(
        url: String,
        headers: List<KeyValue>,
        query: List<KeyValue>,
    ): Result<PassportHttpResponse, PassportException>
}

fun interface PassportPost {
    fun post(
        url: String,
        headers: List<KeyValue>,
        payload: String,
    ): Result<PassportHttpResponse, PassportException>
}

fun interface PassportAuthRegister {
    fun userAuthRegister(
        url: String,
        publicParams: String,
        manifestVersion: String,
    ): Result<CredentialResponse, PassportException>
}

fun interface PassportAuthSubmit {
    fun userAuthSubmit(
        url: String,
        content: String,
        probeCc: String,
        probeAsn: String,
        credentialConfig: SubmitCredentialConfig?,
    ): Result<CredentialResponse, PassportException>
}

fun interface PassportGetProbeId {
    fun getProbeId(
        credentialB64: String,
        probeAsn: String,
        probeCc: String,
    ): Result<String, PassportException>
}
