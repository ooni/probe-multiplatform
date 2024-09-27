package org.ooni.probe.data.models

import org.ooni.engine.Engine

data class DescriptorUpdatesStatus(
    val availableUpdates: List<InstalledTestDescriptorModel> = emptyList(),
    val autoUpdated: List<InstalledTestDescriptorModel> = emptyList(),
    val rejectedUpdates: List<InstalledTestDescriptorModel> = emptyList(),
    val reviewUpdates: List<InstalledTestDescriptorModel> = emptyList(),
    val errors: List<Engine.MkException> = emptyList(),
    val refreshType: UpdateStatusType = UpdateStatusType.None,
) {
    private val availableUpdatesMap = availableUpdates.associateBy { it.id.value }
    private val rejectedUpdatesMap = rejectedUpdates.associateBy { it.id.value }
    private val autoUpdatesMap = autoUpdated.associateBy { it.id.value }

    fun getStatusOf(id: InstalledTestDescriptorModel.Id): UpdateStatus {
        return when {
            autoUpdatesMap.containsKey(id.value) -> UpdateStatus.AutoUpdated
            rejectedUpdatesMap.containsKey(id.value) -> UpdateStatus.UpdateRejected(
                rejectedUpdatesMap[id.value]!!,
            )
            availableUpdatesMap.containsKey(id.value) -> {
                val update = availableUpdatesMap[id.value]!!
                if (update.autoUpdate) UpdateStatus.AutoUpdated else UpdateStatus.Updatable(update)
            }
            else -> UpdateStatus.UpToDate
        }
    }
}
