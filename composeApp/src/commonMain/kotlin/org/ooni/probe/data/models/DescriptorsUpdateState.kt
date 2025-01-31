package org.ooni.probe.data.models

data class DescriptorsUpdateState(
    val availableUpdates: List<InstalledTestDescriptorModel> = emptyList(),
    val autoUpdated: List<InstalledTestDescriptorModel> = emptyList(),
    val operationState: DescriptorUpdateOperationState = DescriptorUpdateOperationState.Idle,
) {
    private val availableUpdatesMap = availableUpdates.associateBy { it.id.value }
    private val autoUpdatesMap = autoUpdated.associateBy { it.id.value }

    fun getStatusOf(id: InstalledTestDescriptorModel.Id): UpdateStatus =
        autoUpdatesMap[id.value]?.let { UpdateStatus.AutoUpdated }
            ?: availableUpdatesMap[id.value]?.let { UpdateStatus.Updatable(it) }
            ?: UpdateStatus.NoNewUpdate
}
