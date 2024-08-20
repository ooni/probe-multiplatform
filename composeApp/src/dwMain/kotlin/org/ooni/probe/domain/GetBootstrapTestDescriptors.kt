package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.ooni.engine.models.OONIRunDescriptor
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.shared.toLocalDateTime

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

    private fun OONIRunDescriptor.toModel() =
        InstalledTestDescriptorModel(
            id = InstalledTestDescriptorModel.Id(oonirunLinkId),
            name = name,
            shortDescription = shortDescription,
            description = description,
            author = author,
            netTests = netTests.map { NetTest.fromOONI(it) },
            nameIntl = nameIntl,
            shortDescriptionIntl = shortDescriptionIntl,
            descriptionIntl = descriptionIntl,
            icon = icon,
            color = color,
            animation = animation,
            expirationDate = expirationDate.toLocalDateTime(),
            dateCreated = dateCreated.toLocalDateTime(),
            dateUpdated = dateUpdated.toLocalDateTime(),
            revision = revision,
            autoUpdate = true,
        )
}
