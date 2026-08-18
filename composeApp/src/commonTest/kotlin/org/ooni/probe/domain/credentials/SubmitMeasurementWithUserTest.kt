package org.ooni.probe.domain.credentials

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Success
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.testing.factories.ManifestFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SubmitMeasurementWithUserTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val measurementData = """{"probe_cc":"US","probe_asn":"AS100"}"""

    private fun buildSubject(response: CredentialResponse) =
        SubmitMeasurementWithUser(
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
        )

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
            val reason = assertIs<PassportException.HttpStatus>(failure.reason)
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
            val reason = assertIs<PassportException.HttpStatus>(failure.reason)
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
            val reason = assertIs<PassportException.HttpStatus>(failure.reason)
            val message = reason.message.orEmpty()
            assertTrue("502" in message, "message should contain the status code: $message")
            assertTrue(retainedBody in message, "message should retain the response prefix: $message")
            assertTrue(omittedBody !in message, "message should bound the response body: $message")
            assertTrue(
                "[protocol_error]" !in message,
                "message should not decode a submit error for an HTTP failure: $message",
            )
        }
}
