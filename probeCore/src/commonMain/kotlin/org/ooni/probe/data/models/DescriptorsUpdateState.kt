package org.ooni.probe.data.models

data class DescriptorsUpdateState(
    val availableUpdates: List<Descriptor> = emptyList(),
    val autoUpdated: List<Descriptor> = emptyList(),
    val operationState: DescriptorUpdateOperationState = DescriptorUpdateOperationState.Idle,
) {
    private val availableUpdatesMap = availableUpdates.associateBy { it.id.value }
    private val autoUpdatesMap = autoUpdated.associateBy { it.id.value }

    fun getStatusOf(id: Descriptor.Id): UpdateStatus =
        autoUpdatesMap[id.value]?.let { UpdateStatus.AutoUpdated }
            ?: availableUpdatesMap[id.value]?.let { UpdateStatus.Updatable(it) }
            ?: UpdateStatus.NoNewUpdate
}
