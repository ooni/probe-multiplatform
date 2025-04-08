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
        val source: Source,
        val netTests: List<NetTest>,
    ) {
        @Serializable
        sealed interface Source {
            @Serializable
            data class Default(val name: String) : Source

            @Serializable
            data class Installed(val id: InstalledTestDescriptorModel.Id) : Source

            companion object {
                fun fromDescriptor(descriptor: Descriptor) =
                    when (descriptor.source) {
                        is Descriptor.Source.Default -> Default(descriptor.name)
                        is Descriptor.Source.Installed -> Installed(descriptor.source.value.id)
                    }
            }
        }
    }

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
        get() = (source as? Test.Source.Default)?.name == "websites"
}
