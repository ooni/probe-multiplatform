package org.ooni.probe.domain.credentials

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetCredentialTest {
    @Test
    fun successful() =
        runTest {
            // Using realistic credential value from Rust test example
            val expectedCredential = "ASAAAAAAAAAAaBoSmSenkROffQvFETMO6MDD5LwxxxD7hfvFrlPv7XIBIAAAAAAAAAAWEktR78DA11bL4SgGPQV3VxeMqbcgE6oXF1CSL4A/JQMAAAAAAAAAIAAAAAAAAACap5+DGII+KNQB7vWB8Cttav7ADisKeRdktfYjXISeSiAAAAAAAAAAhBEiHB7s8COTd4hnoNx1Ouhzu8NFMzA5lS8Lp7wKIiggAAAAAAAAAMJnquClIeYM+Vm7uPq5vVAmkzQOVfG7OoeUFB7QjMtV"

            val getCredential = GetCredential(
                readSecureStorage = { key ->
                    if (key == CredentialsConstants.STORAGE_KEY) {
                        expectedCredential
                    } else {
                        null
                    }
                },
            )

            val result = getCredential()

            assertEquals(expectedCredential, result)
        }

    @Test
    fun noCredentialsStored() =
        runTest {
            val getCredential = GetCredential(
                readSecureStorage = { null },
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
            )

            val result = getCredential()

            assertNull(result)
        }
}
