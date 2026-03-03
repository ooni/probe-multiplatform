package org.ooni.probe.domain.credentials

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.ooni.engine.models.Success
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.data.models.SettingsKey
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
            var manifestStored: String? = null

            // Create GetManifest that returns our test manifest
            val mockGetManifest = GetManifest(
                getPreference = { key ->
                    if (key == SettingsKey.MANIFEST && manifestStored != null) {
                        flowOf(manifestStored)
                    } else {
                        flowOf(null)
                    }
                },
                json = Json.Default,
            )

            // Create RetrieveManifest that "downloads" and stores the manifest
            val mockRetrieveManifest = RetrieveManifest(
                getManifest = { flowOf(null) }, // Initially no manifest
                passportGet = { _, _, _ ->
                    retrieveManifestCalled = true
                    val manifestJson = Json.Default.encodeToString(
                        org.ooni.probe.data.models.Manifest
                            .serializer(),
                        testManifest,
                    )
                    Success(
                        PassportHttpResponse(
                            statusCode = 200,
                            version = "",
                            headersListText = emptyList(),
                            bodyText = manifestJson,
                        ),
                    )
                },
                json = Json.Default,
                setPreference = { key, value ->
                    if (key == SettingsKey.MANIFEST) {
                        manifestStored = value as String
                    }
                },
                backgroundContext = coroutineContext,
            )

            val registerUserWithManifest = RegisterUserWithManifest(
                getManifest = mockGetManifest,
                retrieveManifest = mockRetrieveManifest,
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

            val mockGetManifest = GetManifest(
                getPreference = { flowOf(null) },
                json = Json.Default,
            )

            val mockRetrieveManifest = RetrieveManifest(
                getManifest = { flowOf(null) },
                passportGet = { _, _, _ ->
                    retrieveManifestCalled = true
                    throw RuntimeException("Should not be called")
                },
                json = Json.Default,
                setPreference = { _, _ -> },
                backgroundContext = coroutineContext,
            )

            val registerUserWithManifest = RegisterUserWithManifest(
                getManifest = mockGetManifest,
                retrieveManifest = mockRetrieveManifest,
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
            val mockGetManifest = GetManifest(
                getPreference = { flowOf(null) },
                json = Json.Default,
            )

            val mockRetrieveManifest = RetrieveManifest(
                getManifest = { flowOf(null) },
                passportGet = { _, _, _ ->
                    Success(
                        PassportHttpResponse(
                            statusCode = 404,
                            version = "",
                            headersListText = emptyList(),
                            bodyText = null,
                        ),
                    )
                },
                json = Json.Default,
                setPreference = { _, _ -> },
                backgroundContext = coroutineContext,
            )

            val registerUserWithManifest = RegisterUserWithManifest(
                getManifest = mockGetManifest,
                retrieveManifest = mockRetrieveManifest,
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
            var manifestStored: String? = null

            val mockGetManifest = GetManifest(
                getPreference = { key ->
                    if (key == SettingsKey.MANIFEST && manifestStored != null) {
                        flowOf(manifestStored)
                    } else {
                        flowOf(null)
                    }
                },
                json = Json.Default,
            )

            val mockRetrieveManifest = RetrieveManifest(
                getManifest = { flowOf(null) },
                passportGet = { _, _, _ ->
                    val manifestJson = Json.Default.encodeToString(
                        org.ooni.probe.data.models.Manifest
                            .serializer(),
                        testManifest,
                    )
                    Success(
                        PassportHttpResponse(
                            statusCode = 200,
                            version = "",
                            headersListText = emptyList(),
                            bodyText = manifestJson,
                        ),
                    )
                },
                json = Json.Default,
                setPreference = { key, value ->
                    if (key == SettingsKey.MANIFEST) {
                        manifestStored = value as String
                    }
                },
                backgroundContext = coroutineContext,
            )

            val registerUserWithManifest = RegisterUserWithManifest(
                getManifest = mockGetManifest,
                retrieveManifest = mockRetrieveManifest,
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
            val mockGetManifest = GetManifest(
                getPreference = { flowOf(null) },
                json = Json.Default,
            )

            val mockRetrieveManifest = RetrieveManifest(
                getManifest = { flowOf(null) },
                passportGet = { _, _, _ ->
                    throw RuntimeException("Network error retrieving manifest")
                },
                json = Json.Default,
                setPreference = { _, _ -> },
                backgroundContext = coroutineContext,
            )

            val registerUserWithManifest = RegisterUserWithManifest(
                getManifest = mockGetManifest,
                retrieveManifest = mockRetrieveManifest,
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
            val mockGetManifest = GetManifest(
                getPreference = { flowOf(null) },
                json = Json.Default,
            )

            val mockRetrieveManifest = RetrieveManifest(
                getManifest = { flowOf(null) },
                passportGet = { _, _, _ -> throw RuntimeException("Should not be called") },
                json = Json.Default,
                setPreference = { _, _ -> },
                backgroundContext = coroutineContext,
            )

            val registerUserWithManifest = RegisterUserWithManifest(
                getManifest = mockGetManifest,
                retrieveManifest = mockRetrieveManifest,
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
