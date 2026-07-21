package org.ooni.probe.domain.credentials

import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Success
import org.ooni.passport.PassportGetProbeId
import org.ooni.passport.models.PassportException
import org.ooni.probe.data.models.Credential
import org.ooni.probe.data.models.NetworkModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetAnonymousCredentialsHealthTest {
    @Test
    fun reportsNoCredentialWhenNothingIsStored() =
        runTest {
            val result = subject(credential = null, network = validNetwork)()

            assertEquals(AnonymousCredentialsHealth.NoCredential, result)
        }

    @Test
    fun reportsStoredCredentialWithoutNetworkContext() =
        runTest {
            val result = subject(network = null)()

            assertEquals(AnonymousCredentialsHealth.StoredCredentialWithoutNetwork, result)
        }

    @Test
    fun reportsCredentialThatNeedsResetWhenProbeIdCannotBeDerived() =
        runTest {
            val result = subject(
                getProbeId = PassportGetProbeId { _, _, _ ->
                    Failure(PassportException.InvalidCredential("invalid"))
                },
            )()

            assertEquals(AnonymousCredentialsHealth.CredentialNeedsReset, result)
        }

    @Test
    fun reportsProbeIdForTheLatestValidNetwork() =
        runTest {
            val result = subject()()

            assertIs<AnonymousCredentialsHealth.Ready>(result)
            assertEquals("probe-id", result.probeId)
            assertEquals("AS123", result.probeAsn)
            assertEquals("CM", result.probeCc)
        }

    private fun subject(
        credential: Credential? = Credential("credential", 1u),
        network: NetworkModel? = validNetwork,
        getProbeId: PassportGetProbeId = PassportGetProbeId { _, _, _ -> Success("probe-id") },
    ) = GetAnonymousCredentialsHealth(
        getCredential = { credential },
        getLatestNetwork = { network },
        passportGetProbeId = getProbeId,
    )

    private companion object {
        val validNetwork = NetworkModel(
            name = "Network",
            asn = "AS123",
            countryCode = "CM",
            networkType = null,
        )
    }
}
