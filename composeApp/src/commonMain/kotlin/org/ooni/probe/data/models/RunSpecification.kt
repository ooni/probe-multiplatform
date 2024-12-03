package org.ooni.probe.data.models

import kotlinx.serialization.Serializable
import org.ooni.engine.models.TaskOrigin

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
        val source: InstalledTestDescriptorModel.Id? = null,
        val netTests: List<NetTest>,
    ) {
        companion object {
            fun fromDescriptor(descriptor: Descriptor) = descriptor.source.id
        }
    }
}
