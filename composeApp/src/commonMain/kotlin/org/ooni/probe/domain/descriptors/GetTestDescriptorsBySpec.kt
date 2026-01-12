package org.ooni.probe.domain.descriptors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.DescriptorItem
import org.ooni.probe.data.models.RunSpecification

// Filter TestDescriptors and netTests based on the specification provided
class GetTestDescriptorsBySpec(
    private val getTestDescriptors: () -> Flow<List<DescriptorItem>>,
) {
    suspend operator fun invoke(spec: RunSpecification.Full): List<DescriptorItem> =
        getTestDescriptors()
            .first()
            .filterNot { it.isExpired }
            .mapNotNull { descriptor ->
                val specTest = spec.forDescriptor(descriptor) ?: return@mapNotNull null
                val netTestsWithInputs = getNetTestsWithInputs(specTest, descriptor)

                val specDescriptor = descriptor.copy(
                    source = descriptor.source.copy(
                        netTests = netTestsWithInputs,
                        // long running are already inside netTests
                        longRunningTests = emptyList(),
                    ),
                )

                if (specDescriptor.netTests.isEmpty() && specDescriptor.longRunningTests.isEmpty()) {
                    return@mapNotNull null
                }

                specDescriptor
            }

    // Is this descriptor contained in the RunSpecification's list of tests
    private fun RunSpecification.Full.forDescriptor(descriptor: DescriptorItem) =
        tests.firstOrNull { specTest ->
            specTest.source == descriptor.source.id
        }

    /*
     * If the list of web connectivity inputs (URLs) is empty, it may have been stripped.
     * So we use the list of inputs from the installed database descriptor.
     */
    private fun getNetTestsWithInputs(
        specTest: RunSpecification.Test,
        descriptor: DescriptorItem,
    ) = specTest.netTests.map { specNetTest ->
        if (
            specNetTest.test == TestType.WebConnectivity &&
            specNetTest.inputs.isNullOrEmpty()
        ) {
            specNetTest.copy(
                inputs = descriptor.source.netTests
                    .firstOrNull { it.test == TestType.WebConnectivity }
                    ?.inputs
                    .orEmpty(),
            )
        } else {
            specNetTest
        }
    }
}
