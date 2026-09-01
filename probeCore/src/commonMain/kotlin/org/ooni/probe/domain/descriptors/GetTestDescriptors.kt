package org.ooni.probe.domain.descriptors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.ooni.engine.models.TestType
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
    private val getPreferenceByKey: (SettingsKey) -> Flow<Any?>,
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
            getPreferenceByKey(SettingsKey.DISABLED_TESTS),
        ) { installedDescriptors, descriptorUpdates, isWebsitesEnabled, disabledTestsValue ->
            val updatedDescriptors = installedDescriptors.map { item ->
                item.toDescriptorItem(updateStatus = descriptorUpdates.getStatusOf(item.id))
            }

            @Suppress("UNCHECKED_CAST")
            val disabledTests = (disabledTestsValue as? Set<String>)
                .orEmpty()
                .map { TestType.fromName(it) }
            return@combine updatedDescriptors
                .map { it.copy(enabled = it.name != OoniTest.Websites.key || isWebsitesEnabled) }
                .filterDisabledTests(disabledTests)
                .sortedWith(DescriptorItem.SORT_COMPARATOR)
        }
    }

    private fun isWebsitesDescriptorEnabled() =
        getPreferenceValues(WebConnectivityCategory.entries.mapNotNull { it.settingsKey })
            .map { preferences -> preferences.any { it.value == true } }

    private fun List<DescriptorItem>.filterDisabledTests(disabledTests: List<TestType>) =
        map { item ->
            val descriptor = item.descriptor
            item.copy(
                descriptor = descriptor.copy(
                    netTests = descriptor.netTests
                        .filterNot { disabledTests.contains(it.test) },
                    longRunningTests = descriptor.longRunningTests
                        .filterNot { disabledTests.contains(it.test) },
                ),
            )
        }
}
