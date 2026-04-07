package org.ooni.passport

import org.ooni.engine.models.Result
import org.ooni.passport.PassportBridge.KeyValue
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse

interface PassportBridge :
    PassportGet,
    PassportAuthRegister,
    PassportAuthSubmit,
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
        credential: String,
        publicParams: String,
        content: String,
        probeCc: String,
        probeAsn: String,
        manifestVersion: String,
        age: UInt,
    ): Result<CredentialResponse, PassportException>

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
        credential: String,
        publicParams: String,
        content: String,
        probeCc: String,
        probeAsn: String,
        manifestVersion: String,
        age: UInt,
    ): Result<CredentialResponse, PassportException>
}
