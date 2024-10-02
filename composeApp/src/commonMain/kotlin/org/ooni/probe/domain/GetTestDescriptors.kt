package org.ooni.probe.domain

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_TestOptions_LongRunningTest
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.DefaultTestDescriptor
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorUpdatesStatus
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.UpdateStatus
import org.ooni.probe.data.models.toDescriptor

class GetTestDescriptors(
    private val getDefaultTestDescriptors: () -> List<DefaultTestDescriptor>,
    private val listInstalledTestDescriptors: () -> Flow<List<InstalledTestDescriptorModel>>,
    private val descriptorUpdates: () -> Flow<DescriptorUpdatesStatus>,
) {
    operator fun invoke(): Flow<List<Descriptor>> {
        return combine(
            listInstalledTestDescriptors(),
            descriptorUpdates(),
            flowOf(getDefaultTestDescriptors())
        ) { installedDescriptors, descriptorUpdates , defaultDescriptors ->
            val updatedDescriptors = installedDescriptors.map { item ->
                item.toDescriptor(updateStatus = descriptorUpdates.getStatusOf(item.id))
            }
            return@combine defaultDescriptors
                .map { it.toDescriptor() } + updatedDescriptors
        }
    }

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
        )

    @Composable
    private fun experimentalLinks() =
        """
        * [STUN Reachability](https://github.com/ooni/spec/blob/master/nettests/ts-025-stun-reachability.md)
        * [DNS Check](https://github.com/ooni/spec/blob/master/nettests/ts-028-dnscheck.md)
        * [RiseupVPN](https://ooni.org/nettest/riseupvpn/)
        * [ECH Check](https://github.com/ooni/spec/blob/master/nettests/ts-039-echcheck.md)
        * [Tor Snowflake](https://ooni.org/nettest/tor-snowflake/) (${stringResource(Res.string.Settings_TestOptions_LongRunningTest)})
        * [Vanilla Tor](https://github.com/ooni/spec/blob/master/nettests/ts-016-vanilla-tor.md) (${stringResource(
            Res.string.Settings_TestOptions_LongRunningTest,
        )})
        """.trimIndent()
}
