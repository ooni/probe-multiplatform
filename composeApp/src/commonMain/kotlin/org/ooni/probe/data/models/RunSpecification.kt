package org.ooni.probe.data.models

import org.ooni.engine.models.TaskOrigin

data class RunSpecification(
    val tests: List<Test>,
    val taskOrigin: TaskOrigin,
    val isRerun: Boolean,
) {
    data class Test(
        val source: Source,
        val netTests: List<NetTest>,
    ) {
        sealed interface Source {
            data class Default(val name: String) : Source

            data class Installed(val id: InstalledTestDescriptorModel.Id) : Source
        }
    }
}
