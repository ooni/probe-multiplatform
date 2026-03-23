package org.ooni.passport

import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse

@Suppress("ktlint:standard:function-naming")
fun SuccessPassportHttpResponse(value: PassportHttpResponse) = Success(value) as Result<PassportHttpResponse, PassportException>

@Suppress("ktlint:standard:function-naming")
fun FailureHttpPassportException(reason: PassportException) = Failure(reason) as Result<PassportHttpResponse, PassportException>

@Suppress("ktlint:standard:function-naming")
fun SuccessCredentialResponse(value: CredentialResponse) = Success(value) as Result<CredentialResponse, PassportException>

@Suppress("ktlint:standard:function-naming")
fun FailureCredentialPassportException(reason: PassportException) = Failure(reason) as Result<CredentialResponse, PassportException>
