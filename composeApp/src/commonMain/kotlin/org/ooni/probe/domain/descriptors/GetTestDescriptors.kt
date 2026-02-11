package org.ooni.probe.domain.descriptors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorItem
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.OoniTest
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.toDescriptorItem

class GetTestDescriptors(
    private val listAllInstalledTestDescriptors: () -> Flow<List<Descriptor>>,
    private val listLatestInstalledTestDescriptors: () -> Flow<List<Descriptor>>,
    private val observeDescriptorsUpdateState: () -> Flow<DescriptorsUpdateState>,
    private val getPreferenceValues: (List<SettingsKey>) -> Flow<Map<SettingsKey, Any?>>,
) {
    // Warning: this list will bring duplicated descriptors of different revisions
    fun all(): Flow<List<DescriptorItem>> = get(listAllInstalledTestDescriptors)

    fun latest(): Flow<List<DescriptorItem>> = get(listLatestInstalledTestDescriptors)

    fun single(id: Descriptor.Id): Flow<DescriptorItem?> =
        latest().map { list ->
            list.firstOrNull { it.descriptor.id == id }
        }

    private fun get(installedDescriptorFlow: () -> Flow<List<Descriptor>>): Flow<List<DescriptorItem>> {
        return combine(
            installedDescriptorFlow(),
            observeDescriptorsUpdateState(),
            isWebsitesDescriptorEnabled(),
        ) { installedDescriptors, descriptorUpdates, isWebsitesEnabled ->
            val updatedDescriptors = installedDescriptors.map { item ->
                item.toDescriptorItem(updateStatus = descriptorUpdates.getStatusOf(item.id))
            }
            return@combine updatedDescriptors
                .map {
                    it.copy(enabled = it.name != OoniTest.Websites.key || isWebsitesEnabled)
                }.sortedWith(DescriptorItem.SORT_COMPARATOR)
        }
    }

    private fun isWebsitesDescriptorEnabled() =
        getPreferenceValues(WebConnectivityCategory.entries.mapNotNull { it.settingsKey })
            .map { preferences -> preferences.any { it.value == true } }
}
