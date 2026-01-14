package org.ooni.probe.data.models

import kotlinx.serialization.Serializable
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType

@Serializable
sealed interface RunSpecification {
    @Serializable
    data object OnlyUploadMissingResults : RunSpecification

    @Serializable
    data class Full(
        val tests: List<Test>,
        val taskOrigin: TaskOrigin,
        val isRerun: Boolean,
    ) : RunSpecification

    @Serializable
    data class Test(
        val source: Descriptor.Id,
        val netTests: List<NetTest>,
    )

    /*
     * Remove the URL inputs from the spec if we already have them in our database,
     * because the spec might be to big to pass to a background worker.
     * We will fetch the inputs later in GetTestDescriptorsBySpec.
     * The only case we keep the list of inputs is when the user provides the list in the
     * CustomWebsites screen.
     */
    fun stripInstalledInputs(): RunSpecification =
        when (this) {
            is OnlyUploadMissingResults -> this
            is Full ->
                copy(
                    tests = tests.map { test ->
                        test.copy(
                            netTests = test.netTests.map { netTest ->
                                if (!test.isWebsites && netTest.test == TestType.WebConnectivity) {
                                    netTest.copy(inputs = null)
                                } else {
                                    netTest
                                }
                            },
                        )
                    },
                )
        }

    private val Test.isWebsites
        get() = source.value == OoniTest.Websites.id

    companion object {
        fun buildForDescriptor(
            descriptor: DescriptorItem,
            taskOrigin: TaskOrigin = TaskOrigin.OoniRun,
            isRerun: Boolean = false,
        ) = Full(
            tests = listOf(
                Test(
                    source = descriptor.descriptor.id,
                    netTests = descriptor.allTests,
                ),
            ),
            taskOrigin = taskOrigin,
            isRerun = isRerun,
        )
    }
}
