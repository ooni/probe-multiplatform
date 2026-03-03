package org.ooni.probe.domain.credentials

import kotlinx.coroutines.flow.Flow
import org.ooni.probe.data.models.Manifest

class GetOrRetrieveManifest(
    private val getManifest: GetManifest,
    private val retrieveManifest: RetrieveManifest,
) {
    suspend operator fun invoke(): Flow<Manifest?> {
        retrieveManifest()
        return getManifest()
    }
}
