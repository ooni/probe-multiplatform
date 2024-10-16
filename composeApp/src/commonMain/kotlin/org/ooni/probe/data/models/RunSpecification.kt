package org.ooni.probe.data.models

import kotlinx.serialization.Serializable
import org.ooni.engine.models.TaskOrigin

@Serializable
data class RunSpecification(
    val tests: List<Test>,
    val taskOrigin: TaskOrigin,
    val isRerun: Boolean,
) {
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
}
