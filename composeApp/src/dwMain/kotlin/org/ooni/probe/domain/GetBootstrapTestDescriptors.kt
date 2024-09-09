package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.ooni.engine.models.OONIRunDescriptor
import org.ooni.engine.models.toModel
import org.ooni.probe.data.models.InstalledTestDescriptorModel

class GetBootstrapTestDescriptors(
    private val readAssetFile: (String) -> String,
    private val json: Json,
    private val backgroundDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): List<InstalledTestDescriptorModel> =
        withContext(backgroundDispatcher) {
            val descriptorsJson = readAssetFile("descriptors.json")
            val descriptors =
                try {
                    json.decodeFromString<List<OONIRunDescriptor>>(descriptorsJson)
                } catch (e: Exception) {
                    Logger.e("Could not deserialized bootstrap test descriptors", e)
                    return@withContext emptyList()
                }
            descriptors.map { it.toModel() }
        }
}
