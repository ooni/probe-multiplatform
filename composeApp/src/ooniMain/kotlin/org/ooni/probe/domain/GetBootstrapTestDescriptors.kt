package org.ooni.probe.domain

import kotlinx.serialization.json.Json
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import kotlin.coroutines.CoroutineContext

class GetBootstrapTestDescriptors(
    private val readAssetFile: (String) -> String,
    private val json: Json,
    private val backgroundContext: CoroutineContext,
) {
    operator fun invoke(): List<InstalledTestDescriptorModel> = emptyList()
}
