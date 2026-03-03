package org.ooni.probe.domain.credentials

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
            var retrieveManifestCalled = false
            var registerUserParams: Pair<String, String>? = null

            val registerUserWithManifest = RegisterUserWithManifest(
                retrieveManifest = {
                    retrieveManifestCalled = true
                },
                getManifest = { testManifest },
                getCredentials = { null }, // No existing credentials
                registerUser = { publicParams, manifestVersion ->
                    registerUserParams = publicParams to manifestVersion
                    expectedCredential
                },
                backgroundContext = coroutineContext,
            )

            val result = registerUserWithManifest()

            assertEquals(expectedCredential, result)
            assertEquals(true, retrieveManifestCalled)
            assertEquals(testManifest.manifest.publicParameters to testManifest.meta.version, registerUserParams)
        }

    @Test
    fun returnsExistingCredentials() =
        runTest {
            val existingCredential = "existing_credential_123"
            var retrieveManifestCalled = false
            var registerUserCalled = false

            val registerUserWithManifest = RegisterUserWithManifest(
                retrieveManifest = {
                    retrieveManifestCalled = true
                },
                getManifest = { ManifestFactory.build() },
                getCredentials = { existingCredential }, // Has existing credentials
                registerUser = { _, _ ->
                    registerUserCalled = true
                    "should_not_be_called"
                },
                backgroundContext = coroutineContext,
            )

            val result = registerUserWithManifest()

            assertEquals(existingCredential, result)
            assertEquals(false, retrieveManifestCalled) // Should skip manifest retrieval
            assertEquals(false, registerUserCalled) // Should skip registration
        }

    @Test
    fun noManifestAvailable() =
        runTest {
            val registerUserWithManifest = RegisterUserWithManifest(
                retrieveManifest = { },
                getManifest = { null },
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
            val manifestWithEmptyParams = ManifestFactory.build().copy(
                manifest = ManifestFactory.build().manifest.copy(publicParameters = ""),
            )

            val registerUserWithManifest = RegisterUserWithManifest(
                retrieveManifest = { },
                getManifest = { manifestWithEmptyParams },
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
            val manifestWithEmptyVersion = ManifestFactory.build().copy(
                meta = ManifestFactory.build().meta.copy(version = ""),
            )

            val registerUserWithManifest = RegisterUserWithManifest(
                retrieveManifest = { },
                getManifest = { manifestWithEmptyVersion },
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
            val testManifest = ManifestFactory.build()

            val registerUserWithManifest = RegisterUserWithManifest(
                retrieveManifest = { },
                getManifest = { testManifest },
                getCredentials = { null }, // No existing credentials
                registerUser = { _, _ -> null }, // Registration fails
                backgroundContext = coroutineContext,
            )

            val result = registerUserWithManifest()

            assertNull(result)
        }

    @Test
    fun retrieveManifestThrowsException() =
        runTest {
            val registerUserWithManifest = RegisterUserWithManifest(
                retrieveManifest = {
                    throw RuntimeException("Network error retrieving manifest")
                },
                getManifest = { ManifestFactory.build() },
                getCredentials = { null }, // No existing credentials
                registerUser = { _, _ -> "should_not_be_called" },
                backgroundContext = coroutineContext,
            )

            val result = registerUserWithManifest()

            assertNull(result)
        }

    @Test
    fun getManifestThrowsException() =
        runTest {
            val registerUserWithManifest = RegisterUserWithManifest(
                retrieveManifest = { },
                getManifest = {
                    throw RuntimeException("Error reading manifest from storage")
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
                retrieveManifest = { },
                getManifest = { ManifestFactory.build() },
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
