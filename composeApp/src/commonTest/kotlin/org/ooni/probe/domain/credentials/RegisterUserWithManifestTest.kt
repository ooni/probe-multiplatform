package org.ooni.probe.domain.credentials

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.testing.factories.ManifestFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegisterUserWithManifestTest {
    @Test
    fun successful() =
        runTest {
            val testManifest = ManifestFactory.build()
            val expectedCredential = "test_credential_from_manifest"
            var getOrRetrieveManifestCalled = false
            var registerUserParams: Pair<String, String>? = null

            val registerUserWithManifest = RegisterUserWithManifest(
                getOrRetrieveManifest = {
                    getOrRetrieveManifestCalled = true
                    flowOf(testManifest)
                },
                getCredentials = { null }, // No existing credentials
                registerUser = { publicParams, manifestVersion ->
                    registerUserParams = publicParams to manifestVersion
                    expectedCredential
                },
                backgroundContext = coroutineContext,
            )

            val result = registerUserWithManifest()

            assertEquals(expectedCredential, result)
            assertEquals(true, getOrRetrieveManifestCalled)
            assertEquals(testManifest.manifest.publicParameters to testManifest.meta.version, registerUserParams)
        }

    @Test
    fun returnsExistingCredentials() =
        runTest {
            val existingCredential = "existing_credential_123"
            var getOrRetrieveManifestCalled = false
            var registerUserCalled = false

            val registerUserWithManifest = RegisterUserWithManifest(
                getOrRetrieveManifest = {
                    getOrRetrieveManifestCalled = true
                    flowOf(ManifestFactory.build())
                },
                getCredentials = { existingCredential }, // Has existing credentials
                registerUser = { _, _ ->
                    registerUserCalled = true
                    "should_not_be_called"
                },
                backgroundContext = coroutineContext,
            )

            val result = registerUserWithManifest()

            assertEquals(existingCredential, result)
            assertEquals(false, getOrRetrieveManifestCalled) // Should skip manifest retrieval
            assertEquals(false, registerUserCalled) // Should skip registration
        }

    @Test
    fun noManifestAvailable() =
        runTest {
            val registerUserWithManifest = RegisterUserWithManifest(
                getOrRetrieveManifest = { flowOf(null) },
                getCredentials = { null }, // No existing credentials
                registerUser = { _, _ -> "should_not_be_called" },
                backgroundContext = coroutineContext,
            )

            val result = registerUserWithManifest()

            assertNull(result)
        }

    @Test
    fun emptyPublicParameters() =
        runTest {
            val manifestWithEmptyParams = flowOf(
                ManifestFactory.build().copy(
                    manifest = ManifestFactory.build().manifest.copy(publicParameters = ""),
                ),
            )

            val registerUserWithManifest = RegisterUserWithManifest(
                getOrRetrieveManifest = { manifestWithEmptyParams },
                getCredentials = { null }, // No existing credentials
                registerUser = { _, _ -> "should_not_be_called" },
                backgroundContext = coroutineContext,
            )

            val result = registerUserWithManifest()

            assertNull(result)
        }

    @Test
    fun emptyManifestVersion() =
        runTest {
            val manifestWithEmptyVersion = flowOf(
                ManifestFactory.build().copy(
                    meta = ManifestFactory.build().meta.copy(version = ""),
                ),
            )

            val registerUserWithManifest = RegisterUserWithManifest(
                getOrRetrieveManifest = { manifestWithEmptyVersion },
                getCredentials = { null }, // No existing credentials
                registerUser = { _, _ -> "should_not_be_called" },
                backgroundContext = coroutineContext,
            )

            val result = registerUserWithManifest()

            assertNull(result)
        }

    @Test
    fun registerUserFails() =
        runTest {
            val testManifest = flowOf(ManifestFactory.build())

            val registerUserWithManifest = RegisterUserWithManifest(
                getOrRetrieveManifest = { testManifest },
                getCredentials = { null }, // No existing credentials
                registerUser = { _, _ -> null }, // Registration fails
                backgroundContext = coroutineContext,
            )

            val result = registerUserWithManifest()

            assertNull(result)
        }

    @Test
    fun getOrRetrieveManifestThrowsException() =
        runTest {
            val registerUserWithManifest = RegisterUserWithManifest(
                getOrRetrieveManifest = {
                    throw RuntimeException("Network error retrieving manifest")
                },
                getCredentials = { null }, // No existing credentials
                registerUser = { _, _ -> "should_not_be_called" },
                backgroundContext = coroutineContext,
            )

            val result = registerUserWithManifest()

            assertNull(result)
        }

    @Test
    fun getCredentialsThrowsException() =
        runTest {
            val registerUserWithManifest = RegisterUserWithManifest(
                getOrRetrieveManifest = { flowOf(ManifestFactory.build()) },
                getCredentials = {
                    throw RuntimeException("Error reading credentials from storage")
                },
                registerUser = { _, _ -> "should_not_be_called" },
                backgroundContext = coroutineContext,
            )

            val result = registerUserWithManifest()

            assertNull(result)
        }
}
