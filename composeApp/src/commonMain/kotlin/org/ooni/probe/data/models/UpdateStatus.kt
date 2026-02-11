package org.ooni.probe.data.models

sealed interface UpdateStatus {
    data object Unknown : UpdateStatus

    data object NoNewUpdate : UpdateStatus

    data class Updatable(
        val updatedDescriptor: Descriptor,
    ) : UpdateStatus

    data object AutoUpdated : UpdateStatus

    data object NotApplicable : UpdateStatus
}
