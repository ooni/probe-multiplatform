package org.ooni.probe.domain.credentials

import org.ooni.passport.PassportGetProbeId
import org.ooni.probe.data.models.Credential
import org.ooni.probe.data.models.NetworkModel

class GetAnonymousCredentialsHealth(
    private val getCredential: suspend () -> Credential?,
    private val getLatestNetwork: suspend () -> NetworkModel?,
    private val passportGetProbeId: PassportGetProbeId,
) {
    suspend operator fun invoke(): AnonymousCredentialsHealth {
        val credential = getCredential() ?: return AnonymousCredentialsHealth.NoCredential
        val network = getLatestNetwork()?.takeIf(NetworkModel::isValid)
            ?: return AnonymousCredentialsHealth.StoredCredentialWithoutNetwork

        val probeId = passportGetProbeId
            .getProbeId(
                credentialB64 = credential.credential,
                probeAsn = network.asn.orEmpty(),
                probeCc = network.countryCode.orEmpty(),
            ).get()

        return if (probeId == null) {
            AnonymousCredentialsHealth.CredentialNeedsReset
        } else {
            AnonymousCredentialsHealth.Ready(
                probeId = probeId,
                probeAsn = network.asn.orEmpty(),
                probeCc = network.countryCode.orEmpty(),
            )
        }
    }
}

sealed interface AnonymousCredentialsHealth {
    data object NoCredential : AnonymousCredentialsHealth

    data object StoredCredentialWithoutNetwork : AnonymousCredentialsHealth

    data object CredentialNeedsReset : AnonymousCredentialsHealth

    data class Ready(
        val probeId: String,
        val probeAsn: String,
        val probeCc: String,
    ) : AnonymousCredentialsHealth
}
