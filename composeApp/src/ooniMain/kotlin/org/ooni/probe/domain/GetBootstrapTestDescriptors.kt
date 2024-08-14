package org.ooni.probe.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import org.ooni.probe.data.models.InstalledTestDescriptorModel

class GetBootstrapTestDescriptors(
    private val readAssetFile: (String) -> String,
    private val json: Json,
    private val backgroundDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(): List<InstalledTestDescriptorModel> = emptyList()
}
