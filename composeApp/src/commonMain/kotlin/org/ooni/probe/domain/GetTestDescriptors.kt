package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.DefaultTestDescriptor
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.getCurrent
import org.ooni.probe.shared.hexToColor

class GetTestDescriptors(
    private val getDefaultTestDescriptors: () -> List<DefaultTestDescriptor>,
    private val listInstalledTestDescriptors: () -> Flow<List<InstalledTestDescriptorModel>>,
) {
    operator fun invoke(): Flow<List<Descriptor>> {
        return suspend {
            getDefaultTestDescriptors()
                .map { it.toDescriptor() }
        }.asFlow()
            .flatMapLatest { defaultDescriptors ->
                listInstalledTestDescriptors()
                    .map { list -> list.map { it.toDescriptor() } }
                    .map { defaultDescriptors + it }
            }
    }

    private fun DefaultTestDescriptor.toDescriptor() =
        Descriptor(
            name = label,
            title = { stringResource(title) },
            shortDescription = { stringResource(shortDescription) },
            description = { stringResource(description) },
            icon = icon,
            color = color,
            animation = animation,
            dataUsage = { stringResource(dataUsage) },
            netTests = netTests,
            longRunningTests = longRunningTests,
            source = Descriptor.Source.Default(this),
        )

    private fun InstalledTestDescriptorModel.toDescriptor() =
        Descriptor(
            name = name,
            title = { nameIntl?.getCurrent() ?: name },
            shortDescription = { shortDescriptionIntl?.getCurrent() ?: shortDescription },
            description = { descriptionIntl?.getCurrent() ?: description },
            // TODO: fetch drawable resource from path
            icon = null,
            color = color?.hexToColor(),
            animation = animation,
            dataUsage = { null },
            netTests = netTests.orEmpty(),
            source = Descriptor.Source.Installed(this),
        )
}
