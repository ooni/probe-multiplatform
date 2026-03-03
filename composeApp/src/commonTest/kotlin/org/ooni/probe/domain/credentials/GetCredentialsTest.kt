package org.ooni.probe.domain.credentials

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetCredentialsTest {
    @Test
    fun successful() =
        runTest {
            // Using realistic credential value from Rust test example
            val expectedCredential = "ASAAAAAAAAAAaBoSmSenkROffQvFETMO6MDD5LwxxxD7hfvFrlPv7XIBIAAAAAAAAAAWEktR78DA11bL4SgGPQV3VxeMqbcgE6oXF1CSL4A/JQMAAAAAAAAAIAAAAAAAAACap5+DGII+KNQB7vWB8Cttav7ADisKeRdktfYjXISeSiAAAAAAAAAAhBEiHB7s8COTd4hnoNx1Ouhzu8NFMzA5lS8Lp7wKIiggAAAAAAAAAMJnquClIeYM+Vm7uPq5vVAmkzQOVfG7OoeUFB7QjMtV"

            val getCredentials = GetCredentials(
                readCredentials = { key ->
                    if (key == CredentialsConstants.CREDENTIALS_KEY) {
                        expectedCredential
                    } else {
                        null
                    }
                },
            )

            val result = getCredentials().first()

            assertEquals(expectedCredential, result)
        }

    @Test
    fun noCredentialsStored() =
        runTest {
            val getCredentials = GetCredentials(
                readCredentials = { null },
            )

            val result = getCredentials().first()

            assertNull(result)
        }

    @Test
    fun secureStorageThrowsException() =
        runTest {
            val getCredentials = GetCredentials(
                readCredentials = {
                    throw RuntimeException("Secure storage error")
                },
            )

            val result = getCredentials().first()

            assertNull(result)
        }
}
