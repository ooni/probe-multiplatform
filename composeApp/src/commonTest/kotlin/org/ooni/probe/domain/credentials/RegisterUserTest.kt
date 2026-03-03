package org.ooni.probe.domain.credentials

import kotlinx.coroutines.test.runTest
import org.ooni.engine.WriteResult
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Success
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegisterUserTest {
    companion object {
        // Values from Rust test example
        const val TEST_PUBLIC_PARAMS = "ASAAAAAAAAAAaBoSmSenkROffQvFETMO6MDD5LwxxxD7hfvFrlPv7XIBIAAAAAAAAAAWEktR78DA11bL4SgGPQV3VxeMqbcgE6oXF1CSL4A/JQMAAAAAAAAAIAAAAAAAAACap5+DGII+KNQB7vWB8Cttav7ADisKeRdktfYjXISeSiAAAAAAAAAAhBEiHB7s8COTd4hnoNx1Ouhzu8NFMzA5lS8Lp7wKIiggAAAAAAAAAMJnquClIeYM+Vm7uPq5vVAmkzQOVfG7OoeUFB7QjMtV"
        const val TEST_MANIFEST_VERSION = "3vwveZ4amAz05jqz34w5MQdkOwD03tO8"
    }

    @Test
    fun successful() =
        runTest {
            var storedCredentialKey: String? = null
            var storedCredentialValue: String? = null
            val expectedCredential = "base64_encoded_credential_string"

            val registerUser = RegisterUser(
                userAuthRegister = { _, _, _ ->
                    Success(
                        CredentialResponse(
                            response = PassportHttpResponse(
                                statusCode = 200,
                                version = "",
                                headersListText = emptyList(),
                                bodyText = "success",
                            ),
                            credential = expectedCredential,
                        ),
                    )
                },
                saveCredential = { key, value ->
                    storedCredentialKey = key
                    storedCredentialValue = value
                    WriteResult.Created(key)
                },
                backgroundContext = coroutineContext,
            )

            val result = registerUser(
                publicParams = TEST_PUBLIC_PARAMS,
                manifestVersion = TEST_MANIFEST_VERSION,
            )

            assertEquals(expectedCredential, result)
            assertEquals(CredentialsConstants.CREDENTIALS_KEY, storedCredentialKey)
            assertEquals(expectedCredential, storedCredentialValue)
        }

    @Test
    fun httpError() =
        runTest {
            val registerUser = RegisterUser(
                userAuthRegister = { _, _, _ ->
                    Success(
                        CredentialResponse(
                            response = PassportHttpResponse(
                                statusCode = 400,
                                version = "",
                                headersListText = emptyList(),
                                bodyText = "Bad Request",
                            ),
                            credential = null,
                        ),
                    )
                },
                saveCredential = { _, _ -> WriteResult.Created("") },
                backgroundContext = coroutineContext,
            )

            val result = registerUser(
                publicParams = TEST_PUBLIC_PARAMS,
                manifestVersion = TEST_MANIFEST_VERSION,
            )

            assertNull(result)
        }

    @Test
    fun emptyCredential() =
        runTest {
            val registerUser = RegisterUser(
                userAuthRegister = { _, _, _ ->
                    Success(
                        CredentialResponse(
                            response = PassportHttpResponse(
                                statusCode = 200,
                                version = "",
                                headersListText = emptyList(),
                                bodyText = "success",
                            ),
                            credential = null,
                        ),
                    )
                },
                saveCredential = { _, _ -> WriteResult.Created("") },
                backgroundContext = coroutineContext,
            )

            val result = registerUser(
                publicParams = TEST_PUBLIC_PARAMS,
                manifestVersion = TEST_MANIFEST_VERSION,
            )

            assertNull(result)
        }

    @Test
    fun networkError() =
        runTest {
            val registerUser = RegisterUser(
                userAuthRegister = { _, _, _ ->
                    Failure(PassportException.Other("Network error"))
                },
                saveCredential = { _, _ -> WriteResult.Created("") },
                backgroundContext = coroutineContext,
            )

            val result = registerUser(
                publicParams = TEST_PUBLIC_PARAMS,
                manifestVersion = TEST_MANIFEST_VERSION,
            )

            assertNull(result)
        }

    @Test
    fun storageError() =
        runTest {
            val registerUser = RegisterUser(
                userAuthRegister = { _, _, _ ->
                    Success(
                        CredentialResponse(
                            response = PassportHttpResponse(
                                statusCode = 200,
                                version = "",
                                headersListText = emptyList(),
                                bodyText = "success",
                            ),
                            credential = "test_credential",
                        ),
                    )
                },
                saveCredential = { _, _ ->
                    throw RuntimeException("Storage failed")
                },
                backgroundContext = coroutineContext,
            )

            val result = registerUser(
                publicParams = TEST_PUBLIC_PARAMS,
                manifestVersion = TEST_MANIFEST_VERSION,
            )

            assertNull(result)
        }
}
