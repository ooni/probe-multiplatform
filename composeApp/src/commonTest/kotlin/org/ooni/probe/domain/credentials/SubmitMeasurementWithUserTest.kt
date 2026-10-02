package org.ooni.probe.domain.credentials

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Success
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.passport.models.SubmitError
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.domain.SubmitMeasurement
import org.ooni.testing.factories.ManifestFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SubmitMeasurementWithUserTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val measurementData = """{"probe_cc":"US","probe_asn":"AS100"}"""

    private fun buildSubject(
        response: CredentialResponse,
        onSubmitOutcome: (SubmitError?) -> Unit = {},
    ) = SubmitMeasurementWithUser(
        getManifest = { flowOf(ManifestFactory.build()) },
        getCredential = { null },
        setCredential = SetCredential(
            writeSecureStorage = { _, _ -> error("setCredential should not be used") },
            json = json,
        ),
        stampMeasurement = StampMeasurement(
            passportGetProbeId = { _, _, _ -> error("getProbeId should not be used") },
            getCredential = { null },
            json = json,
        ),
        resolveSubmissionPolicy = ResolveSubmissionPolicy(),
        userAuthSubmit = { _, _, _, _, _ -> Success(response) },
        json = json,
        handleSubmitOutcome = { _, error -> onSubmitOutcome(error) },
    )

    @Test
    fun primaryEndpointIsUsedByDefault() =
        runTest {
            val calledUrls = mutableListOf<String>()
            val subject = SubmitMeasurementWithUser(
                getManifest = { flowOf(ManifestFactory.build()) },
                getCredential = { null },
                setCredential = SetCredential(
                    writeSecureStorage = { _, _ -> error("setCredential should not be used") },
                    json = json,
                ),
                stampMeasurement = StampMeasurement(
                    passportGetProbeId = { _, _, _ -> error("getProbeId should not be used") },
                    getCredential = { null },
                    json = json,
                ),
                resolveSubmissionPolicy = ResolveSubmissionPolicy(),
                userAuthSubmit = { url, _, _, _, _ ->
                    calledUrls.add(url)
                    Success(
                        CredentialResponse(
                            response = PassportHttpResponse(
                                statusCode = 200,
                                version = "HTTP/1.1",
                                headersListText = emptyList(),
                                bodyText = """{"measurement_uid":"uid-123"}""",
                            ),
                            credential = null,
                        ),
                    )
                },
                json = json,
            )

            val result = subject(measurementData)

            val success = assertIs<Success<SubmitMeasurement.ResponseData>>(result)
            assertEquals(MeasurementModel.Uid("uid-123"), success.value.uid)
            assertEquals(
                listOf("${OrganizationConfig.ooniApiBaseUrl}/api/v1/submit_measurement"),
                calledUrls,
            )
        }

    @Test
    fun fallbackEndpointIsAttemptedWhenPrimaryFails() =
        runTest {
            val calledUrls = mutableListOf<String>()
            val subject = SubmitMeasurementWithUser(
                getManifest = { flowOf(ManifestFactory.build()) },
                getCredential = { null },
                setCredential = SetCredential(
                    writeSecureStorage = { _, _ -> error("setCredential should not be used") },
                    json = json,
                ),
                stampMeasurement = StampMeasurement(
                    passportGetProbeId = { _, _, _ -> error("getProbeId should not be used") },
                    getCredential = { null },
                    json = json,
                ),
                resolveSubmissionPolicy = ResolveSubmissionPolicy(),
                userAuthSubmit = { url, _, _, _, _ ->
                    calledUrls.add(url)
                    when {
                        calledUrls.size == 1 && (OrganizationConfig.ooniApiBaseUrl != OrganizationConfig.ooniApiFallbackUrl) ->
                            Failure(PassportException.HttpClientError("Primary failed"))
                        calledUrls.size == 1 ->
                            Failure(PassportException.HttpClientError("Single call failed"))
                        else ->
                            Success(
                                CredentialResponse(
                                    response = PassportHttpResponse(
                                        statusCode = 200,
                                        version = "HTTP/1.1",
                                        headersListText = emptyList(),
                                        bodyText = """{"measurement_uid":"fallback-uid"}""",
                                    ),
                                    credential = null,
                                ),
                            )
                    }
                },
                json = json,
            )

            val result = subject(measurementData)

            if (OrganizationConfig.ooniApiBaseUrl != OrganizationConfig.ooniApiFallbackUrl) {
                val success = assertIs<Success<SubmitMeasurement.ResponseData>>(result)
                assertEquals(MeasurementModel.Uid("fallback-uid"), success.value.uid)
                assertEquals(2, calledUrls.size)
            } else {
                assertIs<Failure<*>>(result)
                assertEquals(1, calledUrls.size)
            }
        }

    @Test
    fun nonSuccessfulResponseSurfacesStatusAndDecodedError() =
        runTest {
            val subject = buildSubject(
                CredentialResponse(
                    response = PassportHttpResponse(
                        statusCode = 500,
                        version = "HTTP/1.1",
                        headersListText = emptyList(),
                        bodyText = """{"error":"protocol_error"}""",
                    ),
                    credential = null,
                ),
            )

            val result = subject(measurementData)

            val failure = assertIs<Failure<*>>(result)
            val reason = assertIs<PassportException.HttpRequestUnsuccessful>(failure.reason)
            val message = reason.message.orEmpty()
            assertEquals(500, reason.statusCode)
            assertTrue("500" in message, "message should contain the status code: $message")
            assertTrue("protocol_error" in message, "message should contain the decoded error/body: $message")
        }

    @Test
    fun nonSuccessfulResponseKeepsRawBodyWhenNotAnErrorEnvelope() =
        runTest {
            val subject = buildSubject(
                CredentialResponse(
                    response = PassportHttpResponse(
                        statusCode = 503,
                        version = "HTTP/1.1",
                        headersListText = emptyList(),
                        bodyText = "upstream unavailable",
                    ),
                    credential = null,
                ),
            )

            val result = subject(measurementData)

            val failure = assertIs<Failure<*>>(result)
            val reason = assertIs<PassportException.HttpRequestUnsuccessful>(failure.reason)
            val message = reason.message.orEmpty()
            assertTrue("503" in message, "message should contain the status code: $message")
            assertTrue("upstream unavailable" in message, "message should retain the raw body: $message")
        }

    @Test
    fun badGatewayDoesNotDecodeSubmitErrorEnvelope() =
        runTest {
            val retainedBody = "{\"error\":\"protocol_error\",\"padding\":\""
            val omittedBody = "x".repeat(501)
            val subject = buildSubject(
                CredentialResponse(
                    response = PassportHttpResponse(
                        statusCode = 502,
                        version = "HTTP/1.1",
                        headersListText = emptyList(),
                        bodyText = retainedBody + omittedBody + "\"}",
                    ),
                    credential = null,
                ),
            )

            val result = subject(measurementData)

            val failure = assertIs<Failure<*>>(result)
            val reason = assertIs<PassportException.HttpRequestUnsuccessful>(failure.reason)
            val message = reason.message.orEmpty()
            assertTrue("502" in message, "message should contain the status code: $message")
            assertTrue(retainedBody in message, "message should retain the response prefix: $message")
            assertTrue(omittedBody !in message, "message should bound the response body: $message")
            assertTrue(
                "[protocol_error]" !in message,
                "message should not decode a submit error for an HTTP failure: $message",
            )
        }

    @Test
    fun credentialAndManifestHttpErrorsTriggerRecoverySignals() =
        runTest {
            val recoveryErrors = mutableListOf<SubmitError?>()
            listOf(
                401 to SubmitError.CredentialError,
                403 to SubmitError.CredentialError,
                404 to SubmitError.ManifestNotFound,
            ).forEach { (statusCode, expectedError) ->
                buildSubject(
                    CredentialResponse(
                        response = PassportHttpResponse(
                            statusCode = statusCode,
                            version = "HTTP/1.1",
                            headersListText = emptyList(),
                            bodyText = null,
                        ),
                        credential = null,
                    ),
                    onSubmitOutcome = { recoveryErrors += it },
                )(measurementData)

                assertEquals(expectedError, recoveryErrors.last())
            }
        }
}
