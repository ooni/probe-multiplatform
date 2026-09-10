package org.ooni.probe.domain.descriptors

import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.ooni.engine.models.Failure
import org.ooni.engine.models.OONINetTest
import org.ooni.engine.models.OONIRunLinkCreateRequest
import org.ooni.engine.models.Success
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.di.Dependencies
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateDescriptorTest {
    private val json = Dependencies.buildJson()
    private val requestJson = Dependencies.buildOonirunRequestJson()
    private val session = AuthSession(
        sessionToken = "jwt-abc",
        emailAddress = "a@example.org",
        role = "user",
        loginTime = Instant.parse("2025-01-01T00:00:00Z"),
    )
    private val request = OONIRunLinkCreateRequest(
        name = "Test",
        shortDescription = "Test",
        description = "Test",
        author = "a@example.org",
        nettests = listOf(OONINetTest(name = "web_connectivity", inputs = listOf("https://ooni.org"))),
    )
    private val createdDescriptorBody = """
        {
            "oonirun_link_id": "1234",
            "name": "Test",
            "short_description": "Test",
            "description": "Test",
            "author": "a@example.org",
            "nettests": [{"test_name": "web_connectivity", "inputs": ["https://ooni.org"]}],
            "name_intl": null,
            "short_description_intl": null,
            "description_intl": null,
            "icon": null,
            "color": null,
            "expiration_date": "2030-01-01T00:00:00Z",
            "date_created": "2025-01-01T00:00:00Z",
            "date_updated": "2025-01-01T00:00:00Z",
            "is_expired": false,
            "revision": "1"
        }
    """.trimIndent()

    @Test
    fun requiresAnAuthenticatedSession() =
        runTest {
            var called = false
            val subject = CreateDescriptor(
                passportPostWithAuth = { _, _, _ ->
                    called = true
                    error("should not be called")
                },
                getStoredSession = { null },
                parseOonirunApiError = ParseOonirunApiError(json),
                requestJson = requestJson,
                responseJson = json,
            )

            val result = subject(request)

            assertTrue(result is Failure)
            assertTrue(result.reason is OonirunApiError.Unauthorized)
            assertTrue(!called)
        }

    @Test
    fun overridesTheAuthorWithTheLoggedInUser() =
        runTest {
            var capturedPayload: String? = null
            val subject = CreateDescriptor(
                passportPostWithAuth = { _, payload, _ ->
                    capturedPayload = payload
                    Success(
                        PassportHttpResponse(
                            statusCode = 200,
                            version = "HTTP/1.1",
                            headersListText = emptyList(),
                            bodyText = createdDescriptorBody,
                        ),
                    )
                },
                getStoredSession = { session },
                parseOonirunApiError = ParseOonirunApiError(json),
                requestJson = requestJson,
                responseJson = json,
            )

            val result = subject(request.copy(author = "someone-else@example.org"))

            assertTrue(result is Success)
            assertTrue(capturedPayload!!.contains("\"author\":\"a@example.org\""))
            assertTrue(!capturedPayload!!.contains("someone-else@example.org"))
        }

    @Test
    fun attachesBearerTokenAndDecodesCreatedDescriptor() =
        runTest {
            var authHeaderValue: String? = null
            val subject = CreateDescriptor(
                passportPostWithAuth = { _, _, headers ->
                    authHeaderValue = headers.first { it.key == "Authorization" }.value
                    Success(
                        PassportHttpResponse(
                            statusCode = 200,
                            version = "HTTP/1.1",
                            headersListText = emptyList(),
                            bodyText = createdDescriptorBody,
                        ),
                    )
                },
                getStoredSession = { session },
                parseOonirunApiError = ParseOonirunApiError(json),
                requestJson = requestJson,
                responseJson = json,
            )

            val result = subject(request)

            assertTrue(result is Success)
            assertEquals("1234", result.value.id.value)
            assertEquals(1L, result.value.revision)
            assertEquals("Bearer jwt-abc", authHeaderValue)
        }

    @Test
    fun omitsUnsetOptionalFieldsFromThePayloadInsteadOfSendingExplicitNulls() =
        runTest {
            var capturedPayload: String? = null
            val subject = CreateDescriptor(
                passportPostWithAuth = { _, payload, _ ->
                    capturedPayload = payload
                    Success(
                        PassportHttpResponse(
                            statusCode = 200,
                            version = "HTTP/1.1",
                            headersListText = emptyList(),
                            bodyText = createdDescriptorBody,
                        ),
                    )
                },
                getStoredSession = { session },
                parseOonirunApiError = ParseOonirunApiError(json),
                requestJson = requestJson,
                responseJson = json,
            )

            subject(request)

            val payload = capturedPayload!!
            assertTrue(payload.contains("\"name\":\"Test\""))
            assertTrue(payload.contains("\"nettests\""))
            // name_intl, icon, color, expiration_date, etc. are all unset - none should appear
            // as explicit nulls, or the server's own defaults (e.g. expiration_date = now+6mo)
            // never kick in.
            assertTrue(!payload.contains("null"))
        }

    @Test
    fun mapsServerValidationErrorToTypedError() =
        runTest {
            val subject = CreateDescriptor(
                passportPostWithAuth = { _, _, _ ->
                    Success(
                        PassportHttpResponse(
                            statusCode = 422,
                            version = "HTTP/1.1",
                            headersListText = emptyList(),
                            bodyText = """{"detail": {"error": "targetsName cannot be combined with inputs"}}""",
                        ),
                    )
                },
                getStoredSession = { session },
                parseOonirunApiError = ParseOonirunApiError(json),
                requestJson = requestJson,
                responseJson = json,
            )

            val result = subject(request)

            assertTrue(result is Failure)
            assertTrue(result.reason is OonirunApiError.InvalidNetTests)
        }
}
