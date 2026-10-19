package org.ooni.probe.domain.descriptors

import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.ooni.engine.models.Failure
import org.ooni.engine.models.OONINetTest
import org.ooni.engine.models.OONIRunLinkCreateRequest
import org.ooni.engine.models.Success
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.di.Dependencies
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateDescriptorTest {
    private val json = Dependencies.buildJson()
    private val requestJson = Dependencies.buildOonirunRequestJson()
    private val session = AuthSession(
        sessionToken = "jwt-abc",
        emailAddress = "a@example.org",
        role = "user",
        loginTime = Instant.parse("2025-01-01T00:00:00Z"),
    )
    private val descriptorId = Descriptor.Id("1234")
    private val request = OONIRunLinkCreateRequest(
        name = "Test",
        shortDescription = "Test",
        description = "Test",
        author = "a@example.org",
        nettests = listOf(OONINetTest(name = "web_connectivity", inputs = listOf("https://ooni.org"))),
    )
    private val updatedDescriptorBody = """
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
            "date_updated": "2025-06-01T00:00:00Z",
            "is_expired": false,
            "revision": "2"
        }
    """.trimIndent()

    @Test
    fun requiresAnAuthenticatedSession() =
        runTest {
            var called = false
            val subject = UpdateDescriptor(
                passportPutWithAuth = { _, _, _ ->
                    called = true
                    error("should not be called")
                },
                getStoredSession = { null },
                parseOonirunApiError = ParseOonirunApiError(json),
                requestJson = requestJson,
                responseJson = json,
            )

            val result = subject(descriptorId, request)

            assertTrue(result is Failure)
            assertTrue(result.reason is OonirunApiError.Unauthorized)
            assertTrue(!called)
        }

    @Test
    fun putsToTheDescriptorUrlWithBearerTokenAndDecodesResponse() =
        runTest {
            var capturedUrl: String? = null
            var authHeaderValue: String? = null
            val subject = UpdateDescriptor(
                passportPutWithAuth = { url, _, headers ->
                    capturedUrl = url
                    authHeaderValue = headers.first { it.key == "Authorization" }.value
                    Success(
                        PassportHttpResponse(
                            statusCode = 200,
                            version = "HTTP/1.1",
                            headersListText = emptyList(),
                            bodyText = updatedDescriptorBody,
                        ),
                    )
                },
                getStoredSession = { session },
                parseOonirunApiError = ParseOonirunApiError(json),
                requestJson = requestJson,
                responseJson = json,
            )

            val result = subject(descriptorId, request)

            assertTrue(result is Success)
            assertEquals("1234", result.value.id.value)
            assertEquals(2L, result.value.revision)
            assertTrue(capturedUrl!!.endsWith("/api/v2/oonirun/links/1234"))
            assertEquals("Bearer jwt-abc", authHeaderValue)
        }

    @Test
    fun overridesTheAuthorWithTheLoggedInUser() =
        runTest {
            var capturedPayload: String? = null
            val subject = UpdateDescriptor(
                passportPutWithAuth = { _, payload, _ ->
                    capturedPayload = payload
                    Success(
                        PassportHttpResponse(
                            statusCode = 200,
                            version = "HTTP/1.1",
                            headersListText = emptyList(),
                            bodyText = updatedDescriptorBody,
                        ),
                    )
                },
                getStoredSession = { session },
                parseOonirunApiError = ParseOonirunApiError(json),
                requestJson = requestJson,
                responseJson = json,
            )

            val result = subject(descriptorId, request.copy(author = "someone-else@example.org"))

            assertTrue(result is Success)
            assertTrue(capturedPayload!!.contains("\"author\":\"a@example.org\""))
            assertTrue(!capturedPayload!!.contains("someone-else@example.org"))
        }

    @Test
    fun mapsForbiddenToTypedError() =
        runTest {
            val subject = UpdateDescriptor(
                passportPutWithAuth = { _, _, _ ->
                    Success(
                        PassportHttpResponse(
                            statusCode = 403,
                            version = "HTTP/1.1",
                            headersListText = emptyList(),
                            bodyText = """{"detail": "You do not have permission to edit this link"}""",
                        ),
                    )
                },
                getStoredSession = { session },
                parseOonirunApiError = ParseOonirunApiError(json),
                requestJson = requestJson,
                responseJson = json,
            )

            val result = subject(descriptorId, request)

            assertTrue(result is Failure)
            assertTrue(result.reason is OonirunApiError.Forbidden)
        }
}
