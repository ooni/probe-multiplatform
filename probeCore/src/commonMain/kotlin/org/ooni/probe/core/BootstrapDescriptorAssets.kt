package org.ooni.probe.core

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import org.ooni.engine.models.OONIRunDescriptor
import org.ooni.engine.models.toModel
import org.ooni.probe.data.models.Descriptor

/**
 * Shared bootstrap-descriptor loading, used by both composeApp (Compose-resource-backed provider)
 * and cliApp (classpath-resource-backed provider).
 */
enum class DescriptorAssetSet {
    Common,
    Ooni,
    Dw,
    ;

    companion object {
        val cliDefault = Ooni
    }
}

fun interface DescriptorAssetProvider {
    suspend fun load(assetSet: DescriptorAssetSet): String
}

class BootstrapDescriptorDecoder(
    private val json: Json,
) {
    suspend fun decode(
        assetProvider: DescriptorAssetProvider,
        assetSet: DescriptorAssetSet,
    ): List<Descriptor> =
        try {
            json.decodeFromString<List<OONIRunDescriptor>>(assetProvider.load(assetSet)).map { it.toModel() }
        } catch (error: Exception) {
            Logger.e("Could not deserialize bootstrap test descriptors", error)
            emptyList()
        }
}
