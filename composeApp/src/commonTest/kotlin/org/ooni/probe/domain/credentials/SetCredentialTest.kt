package org.ooni.probe.domain.credentials

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.ooni.engine.WriteResult
import org.ooni.probe.data.models.Credential
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetCredentialTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val credential = Credential(credential = "credential", emissionDay = 42u)

    @Test
    fun created() =
        runTest {
            var storedValue: String? = null
            val setCredential = SetCredential(
                writeSecureStorage = { key, value ->
                    assertEquals(CredentialsConstants.STORAGE_KEY, key)
                    storedValue = value
                    WriteResult.Created(key)
                },
                json = json,
            )

            assertTrue(setCredential(credential))
            assertEquals(json.encodeToString(Credential.serializer(), credential), storedValue)
        }

    @Test
    fun error() =
        runTest {
            val setCredential = SetCredential(
                writeSecureStorage = { key, _ -> WriteResult.Error(key, "failure") },
                json = json,
            )

            assertFalse(setCredential(credential))
        }

    @Test
    fun temporarilyUnavailable() =
        runTest {
            val setCredential = SetCredential(
                writeSecureStorage = { key, _ -> WriteResult.TemporarilyUnavailable(key) },
                json = json,
            )

            assertFalse(setCredential(credential))
        }

    @Test
    fun exception() =
        runTest {
            val setCredential = SetCredential(
                writeSecureStorage = { _, _ -> throw IllegalStateException("failure") },
                json = json,
            )

            assertFalse(setCredential(credential))
        }
}
