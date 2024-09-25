package org.ooni.probe.data.models

import org.ooni.engine.Engine

data class DescriptorUpdatesStatus(
    val availableUpdates: List<InstalledTestDescriptorModel> = emptyList(),
    val autoUpdated: List<InstalledTestDescriptorModel> = emptyList(),
    val rejectedUpdates: List<InstalledTestDescriptorModel> = emptyList(),
    val reviewUpdates: List<InstalledTestDescriptorModel> = emptyList(),
    val errors: List<Engine.MkException> = emptyList(),
    val refreshType: UpdateStatusType = UpdateStatusType.None,
)
