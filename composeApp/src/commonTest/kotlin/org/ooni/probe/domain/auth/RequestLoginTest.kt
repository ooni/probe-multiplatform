package org.ooni.probe.domain.auth

import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Success
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.di.Dependencies
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequestLoginTest {
    private val json = Dependencies.buildJson()

    @Test
    fun sendsEmailAndRedirectToAndDecodesResponse() =
        runTest {
            var capturedUrl: String? = null
            var capturedPayload: String? = null
            val subject = RequestLogin(
                passportPost = { url, payload ->
                    capturedUrl = url
                    capturedPayload = payload
                    Success(
                        PassportHttpResponse(
                            statusCode = 200,
                            version = "HTTP/1.1",
                            headersListText = emptyList(),
                            bodyText = """{"email_address":"a@example.org","login_token_expiration":"2030-01-01T00:00:00Z"}""",
                        ),
                    )
                },
                json = json,
            )

            val result = subject("a@example.org")

            assertTrue(result is Success)
            assertEquals("a@example.org", result.value.emailAddress)
            assertTrue(capturedUrl!!.endsWith("/api/v2/ooniauth/user-login"))
            assertTrue(capturedPayload!!.contains("\"email_address\":\"a@example.org\""))
            assertTrue(capturedPayload!!.contains("\"redirect_to\""))
        }

    @Test
    fun mapsNetworkFailure() =
        runTest {
            val subject = RequestLogin(
                passportPost = { _, _ -> Failure(PassportException.Offline("no network")) },
                json = json,
            )

            val result = subject("a@example.org")

            assertTrue(result is Failure)
            assertTrue(result.reason is AuthException.Network)
        }

    @Test
    fun mapsHttpErrorResponse() =
        runTest {
            val subject = RequestLogin(
                passportPost = { _, _ ->
                    Success(
                        PassportHttpResponse(
                            statusCode = 429,
                            version = "HTTP/1.1",
                            headersListText = emptyList(),
                            bodyText = """{"error":"quota exceeded"}""",
                        ),
                    )
                },
                json = json,
            )

            val result = subject("a@example.org")

            assertTrue(result is Failure)
            val reason = result.reason
            assertTrue(reason is AuthException.Http)
            assertEquals(429, reason.statusCode)
        }
}
