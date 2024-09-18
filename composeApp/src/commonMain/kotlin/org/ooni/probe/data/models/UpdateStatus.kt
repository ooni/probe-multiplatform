package org.ooni.probe.data.models

sealed class UpdateStatus {
    data object Unknown : UpdateStatus()

    data object UpToDate : UpdateStatus()

    data class Updatable(val updatedDescriptor: InstalledTestDescriptorModel) : UpdateStatus()

    data object AutoUpdated : UpdateStatus()

    data object NotApplicable : UpdateStatus()

    data class UpdateRejected(val updatedDescriptor: InstalledTestDescriptorModel) : UpdateStatus()
}
