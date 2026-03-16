package org.ooni.probe.domain.credentials

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.ooni.probe.data.models.Credential
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetCredentialTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun successful() =
        runTest {
            val storedJson = """{"credential_sign_response":"test_credential","emission_day":42}"""

            val getCredential = GetCredential(
                readSecureStorage = { key ->
                    if (key == CredentialsConstants.STORAGE_KEY) {
                        storedJson
                    } else {
                        null
                    }
                },
                json = json,
            )

            val result = getCredential()

            assertEquals(
                Credential(credential = "test_credential", emissionDay = 42u),
                result,
            )
        }

    @Test
    fun noCredentialsStored() =
        runTest {
            val getCredential = GetCredential(
                readSecureStorage = { null },
                json = json,
            )

            val result = getCredential()

            assertNull(result)
        }

    @Test
    fun invalidJson() =
        runTest {
            val getCredential = GetCredential(
                readSecureStorage = { "not valid json" },
                json = json,
            )

            val result = getCredential()

            assertNull(result)
        }

    @Test
    fun secureStorageThrowsException() =
        runTest {
            val getCredential = GetCredential(
                readSecureStorage = {
                    throw RuntimeException("Secure storage error")
                },
                json = json,
            )

            val result = getCredential()

            assertNull(result)
        }
}
