package org.ooni.probe.domain.auth

import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Success
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.di.Dependencies
import org.ooni.testing.factories.PassportHttpResponseFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefreshSessionTest {
    private val json = Dependencies.buildJson()
    private val existing = AuthSession(
        sessionToken = "old-jwt",
        emailAddress = "a@example.org",
        role = "user",
        loginTime = Instant.parse("2025-01-01T00:00:00Z"),
    )

    @Test
    fun failsFastWhenNotLoggedIn() =
        runTest {
            var called = false
            val subject = RefreshSession(
                passportPostWithAuth = { _, _, _ ->
                    called = true
                    error("should not be called")
                },
                getStoredSession = { null },
                storeSession = { true },
                clearSession = { },
                decodeAuthResponse = ::decodeAuthResponse,
            )

            val result = subject()

            assertTrue(result is Failure)
            assertTrue(result.reason is AuthException.NotLoggedIn)
            assertFalse(called)
        }

    @Test
    fun attachesBearerTokenAndStoresRefreshedSession() =
        runTest {
            var stored: AuthSession? = null
            var authHeaderValue: String? = null
            val subject = RefreshSession(
                passportPostWithAuth = { _, _, headers ->
                    authHeaderValue = headers.first { it.key == "Authorization" }.value
                    Success(
                        PassportHttpResponseFactory.successful(
                            bodyText = """
                                {
                                    "session_token": "new-jwt",
                                    "email_address": "a@example.org",
                                    "role": "user",
                                    "login_time": "2025-01-01T00:00:00Z",
                                    "is_logged_in": true
                                }
                            """.trimIndent(),
                        ),
                    )
                },
                getStoredSession = { existing },
                storeSession = {
                    stored = it
                    true
                },
                clearSession = { error("should not clear on success") },
                decodeAuthResponse = ::decodeAuthResponse,
            )

            val result = subject()

            assertTrue(result is Success)
            assertEquals("new-jwt", result.value.sessionToken)
            assertEquals("Bearer old-jwt", authHeaderValue)
            assertEquals("new-jwt", stored?.sessionToken)
        }

    @Test
    fun clearsSessionOn401() =
        runTest {
            var cleared = false
            val subject = RefreshSession(
                passportPostWithAuth = { _, _, _ ->
                    Success(
                        PassportHttpResponseFactory.error(
                            statusCode = 401,
                            bodyText = "Authentication required",
                        ),
                    )
                },
                getStoredSession = { existing },
                storeSession = { error("should not store on failure") },
                clearSession = { cleared = true },
                decodeAuthResponse = ::decodeAuthResponse,
            )

            val result = subject()

            assertTrue(result is Failure)
            assertTrue(cleared)
        }
}
