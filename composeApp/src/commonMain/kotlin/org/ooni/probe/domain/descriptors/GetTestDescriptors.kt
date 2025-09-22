package org.ooni.probe.domain.descriptors

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_TestOptions_LongRunningTest
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.DefaultTestDescriptor
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.UpdateStatus
import org.ooni.probe.data.models.toDescriptor

class GetTestDescriptors(
    private val getDefaultTestDescriptors: () -> List<DefaultTestDescriptor>,
    private val listAllInstalledTestDescriptors: () -> Flow<List<InstalledTestDescriptorModel>>,
    private val listLatestInstalledTestDescriptors: () -> Flow<List<InstalledTestDescriptorModel>>,
    private val observeDescriptorsUpdateState: () -> Flow<DescriptorsUpdateState>,
    private val getPreferenceValues: (List<SettingsKey>) -> Flow<Map<SettingsKey, Any?>>,
) {
    // Warning: this list will bring duplicated descriptors of different revisions
    fun all(): Flow<List<Descriptor>> = get(listAllInstalledTestDescriptors)

    fun latest(): Flow<List<Descriptor>> = get(listLatestInstalledTestDescriptors)

    fun single(key: String) =
        latest().map { list ->
            list.firstOrNull { it.key == key }
        }

    private fun get(installedDescriptorFlow: () -> Flow<List<InstalledTestDescriptorModel>>): Flow<List<Descriptor>> {
        return combine(
            installedDescriptorFlow(),
            observeDescriptorsUpdateState(),
            flowOf(getDefaultTestDescriptors()),
            isWebsitesDescriptorEnabled(),
        ) { installedDescriptors, descriptorUpdates, defaultDescriptors, isWebsitesEnabled ->
            val updatedDescriptors = installedDescriptors.map { item ->
                item.toDescriptor(updateStatus = descriptorUpdates.getStatusOf(item.id))
            }
            val allDescriptors = defaultDescriptors.map { it.toDescriptor() } + updatedDescriptors
            return@combine allDescriptors.map {
                it.copy(enabled = it.name != "websites" || isWebsitesEnabled)
            }
        }
    }

    private fun isWebsitesDescriptorEnabled() =
        getPreferenceValues(WebConnectivityCategory.entries.mapNotNull { it.settingsKey })
            .map { preferences -> preferences.any { it.value == true } }

    private fun DefaultTestDescriptor.toDescriptor() =
        Descriptor(
            name = label,
            title = { stringResource(title) },
            shortDescription = { stringResource(shortDescription) },
            description = {
                if (label == "experimental") {
                    stringResource(description, experimentalLinks())
                } else {
                    stringResource(description)
                }
            },
            icon = icon,
            color = color,
            animation = animation,
            dataUsage = { stringResource(dataUsage) },
            expirationDate = null,
            netTests = netTests,
            longRunningTests = longRunningTests,
            source = Descriptor.Source.Default(this),
            updateStatus = UpdateStatus.NotApplicable,
            summaryType = summaryType,
        )

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
