package org.ooni.probe.domain.descriptors

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_TestOptions_LongRunningTest
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.DescriptorItem
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

    fun single(key: String): Flow<DescriptorItem?> =
        latest().map { list ->
            list.firstOrNull { it.key == key }
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

    @Composable
    private fun experimentalLinks() =
        """
        * [STUN Reachability](https://github.com/ooni/spec/blob/master/nettests/ts-025-stun-reachability.md)
        * [OpenVPN](https://github.com/ooni/spec/blob/master/nettests/ts-040-openvpn.md)
        * [ECH Check](https://github.com/ooni/spec/blob/master/nettests/ts-039-echcheck.md)
        * [DNS Check](https://github.com/ooni/spec/blob/master/nettests/ts-028-dnscheck.md)
        * [Tor Snowflake](https://ooni.org/nettest/tor-snowflake/) (${stringResource(Res.string.Settings_TestOptions_LongRunningTest)})
        * [Vanilla Tor](https://github.com/ooni/spec/blob/master/nettests/ts-016-vanilla-tor.md) (${
            stringResource(
                Res.string.Settings_TestOptions_LongRunningTest,
            )
        })
        """.trimIndent()
}
