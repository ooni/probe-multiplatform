package org.ooni.probe.data.models

import org.ooni.engine.Engine

data class DescriptorUpdatesStatus(
    val availableUpdates: Set<InstalledTestDescriptorModel> = emptySet(),
    val autoUpdated: Set<InstalledTestDescriptorModel> = emptySet(),
    val rejectedUpdates: Set<InstalledTestDescriptorModel> = emptySet(),
    val reviewUpdates: Set<InstalledTestDescriptorModel> = emptySet(),
    val errors: List<Engine.MkException> = emptyList(),
    val refreshType: UpdateStatusType = UpdateStatusType.None,
)
