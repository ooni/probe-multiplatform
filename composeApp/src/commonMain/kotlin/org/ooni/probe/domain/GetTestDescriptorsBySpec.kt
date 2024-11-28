package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.RunSpecification

// Filter TestDescriptors and netTests based on the specification provided
class GetTestDescriptorsBySpec(
    private val getTestDescriptors: () -> Flow<List<Descriptor>>,
) {
    suspend operator fun invoke(spec: RunSpecification.Full): List<Descriptor> =
        getTestDescriptors()
            .first()
            .filterNot { it.isExpired }
            .mapNotNull { descriptor ->
                val specTest = spec.forDescriptor(descriptor) ?: return@mapNotNull null

                val specDescriptor = descriptor.copy(
                    netTests = specTest.netTests,
                    // long running are already inside netTests
                    longRunningTests = emptyList(),
                )

                if (specDescriptor.netTests.isEmpty() && specDescriptor.longRunningTests.isEmpty()) {
                    return@mapNotNull null
                }

                specDescriptor
            }

    // Is this descriptor contained in the RunSpecification's list of tests
    private fun RunSpecification.Full.forDescriptor(descriptor: Descriptor) =
        tests.firstOrNull { specTest ->
            when (descriptor.source) {
                is Descriptor.Source.Default -> {
                    specTest.source is RunSpecification.Test.Source.Default &&
                        specTest.source.name == descriptor.name
                }

                is Descriptor.Source.Installed -> {
                    specTest.source is RunSpecification.Test.Source.Installed &&
                        specTest.source.id == descriptor.source.value.id
                }
            }
        }
}
