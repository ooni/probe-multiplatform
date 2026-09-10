package org.ooni.probe.domain.auth

import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Success
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.di.Dependencies
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExchangeLoginTokenTest {
    private val json = Dependencies.buildJson()

    @Test
    fun storesSessionOnSuccess() =
        runTest {
            var stored: AuthSession? = null
            val subject = ExchangeLoginToken(
                passportPost = { _, payload ->
                    assertTrue(payload.contains("\"login_token\":\"the-token\""))
                    Success(
                        PassportHttpResponse(
                            statusCode = 200,
                            version = "HTTP/1.1",
                            headersListText = emptyList(),
                            bodyText = """
                                {
                                    "session_token": "jwt-abc",
                                    "redirect_to": null,
                                    "email_address": "a@example.org",
                                    "account_id": "acc1",
                                    "role": "user",
                                    "login_time": "2025-01-01T00:00:00Z",
                                    "is_logged_in": true
                                }
                            """.trimIndent(),
                        ),
                    )
                },
                storeSession = {
                    stored = it
                    true
                },
                json = json,
            )

            val result = subject("the-token")

            assertTrue(result is Success)
            assertEquals("jwt-abc", result.value.sessionToken)
            assertEquals("a@example.org", result.value.emailAddress)
            assertEquals(stored, result.value)
        }

    @Test
    fun doesNotStoreWhenNotLoggedIn() =
        runTest {
            var stored: AuthSession? = null
            val subject = ExchangeLoginToken(
                passportPost = { _, _ ->
                    Success(
                        PassportHttpResponse(
                            statusCode = 200,
                            version = "HTTP/1.1",
                            headersListText = emptyList(),
                            bodyText = """{"is_logged_in": false}""",
                        ),
                    )
                },
                storeSession = {
                    stored = it
                    true
                },
                json = json,
            )

            val result = subject("expired-token")

            assertTrue(result is Failure)
            assertTrue(result.reason is AuthException.Decode)
            assertNull(stored)
        }
}
