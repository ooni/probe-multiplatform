package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ooniprobe.composeapp.generated.resources.Res
import org.ooni.engine.models.OONIRunDescriptor
import org.ooni.engine.models.toModel
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import kotlin.coroutines.CoroutineContext

class GetBootstrapTestDescriptors(
    private val readAssetFile: (String) -> String,
    private val json: Json,
    private val backgroundContext: CoroutineContext,
) {
    suspend operator fun invoke(): List<InstalledTestDescriptorModel> =
        withContext(backgroundContext) {

            val descriptorsJson = Res.readBytes("files/assets/descriptors.json").decodeToString()
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
