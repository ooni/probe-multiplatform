package org.ooni.probe.domain.descriptors

import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ooniprobe.composeapp.generated.resources.Res
import org.ooni.probe.core.BootstrapDescriptorDecoder
import org.ooni.probe.core.DescriptorAssetProvider
import org.ooni.probe.core.DescriptorAssetSet
import org.ooni.probe.data.models.Descriptor
import kotlin.coroutines.CoroutineContext

class GetBootstrapTestDescriptors(
    private val json: Json,
    private val backgroundContext: CoroutineContext,
) {
    private val decoder = BootstrapDescriptorDecoder(json)

    suspend operator fun invoke(): List<Descriptor> =
        withContext(backgroundContext) {
            decoder.decode(ComposeDescriptorAssetProvider, DescriptorAssetSet.Ooni)
        }
}

private object ComposeDescriptorAssetProvider : DescriptorAssetProvider {
    override suspend fun load(assetSet: DescriptorAssetSet): String {
        check(assetSet == DescriptorAssetSet.Ooni) { "Compose resources select the active organization asset" }
        return Res.readBytes("files/assets/descriptors.json").decodeToString()
    }
}
