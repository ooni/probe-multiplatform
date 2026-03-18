package org.ooni.probe.domain.credentials

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.Credential
import org.ooni.testing.factories.ManifestFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PrepareAnonymousCredentialsTest {
    @Test
    fun successful() =
        runTest {
            val manifest = ManifestFactory.build()
            val expectedCredential = Credential(credential = "test_credential_from_manifest", emissionDay = 42u)
            var registerUserParams: Pair<String, String>? = null
            val subject = PrepareAnonymousCredentials(
                getManifest = { flowOf(manifest) },
                retrieveManifest = { null },
                getCredential = { null }, // No existing credentials
                registerUser = { publicParams, manifestVersion ->
                    registerUserParams = publicParams to manifestVersion
                    expectedCredential
                },
                backgroundContext = coroutineContext,
            )

            val result = subject()

            assertEquals(expectedCredential, result)
            assertEquals(manifest.manifest.publicParameters to manifest.meta.version, registerUserParams)
        }

    @Test
    fun returnsExistingCredentials() =
        runTest {
            val existingCredential = Credential(
                credential = "existing_credential_123",
                emissionDay = 42u,
            )
            var retrieveManifestCalled = false
            var registerUserCalled = false
            val subject = PrepareAnonymousCredentials(
                getManifest = { flowOf(null) },
                retrieveManifest = {
                    retrieveManifestCalled = true
                    null
                },
                getCredential = { existingCredential }, // Has existing credentials
                registerUser = { _, _ ->
                    registerUserCalled = true
                    null
                },
                backgroundContext = coroutineContext,
            )

            val result = subject()

            assertEquals(existingCredential, result)
            assertEquals(false, retrieveManifestCalled) // Should skip manifest retrieval
            assertEquals(false, registerUserCalled) // Should skip registration
        }

    @Test
    fun noManifestAvailable() =
        runTest {
            val subject = PrepareAnonymousCredentials(
                getManifest = { flowOf(null) },
                retrieveManifest = { null },
                getCredential = { null }, // No existing credentials
                registerUser = { _, _ -> null },
                backgroundContext = coroutineContext,
            )

            val result = subject()

            assertNull(result)
        }

    @Test
    fun registerUserFails() =
        runTest {
            val testManifest = ManifestFactory.build()
            val subject = PrepareAnonymousCredentials(
                getManifest = { flowOf(testManifest) },
                retrieveManifest = { null },
                getCredential = { null }, // No existing credentials
                registerUser = { _, _ -> null }, // Registration fails
                backgroundContext = coroutineContext,
            )

            val result = subject()

            assertNull(result)
        }
}
