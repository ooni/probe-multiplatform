package org.ooni.probe.background

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import platform.Foundation.NSOperation

class DescriptorUpdateOperation(
    private val descriptors: List<InstalledTestDescriptorModel>? = null,
    private val fetchDescriptorsUpdates: suspend (List<InstalledTestDescriptorModel>) -> Unit,
) : NSOperation() {
    override fun main() {
        super.main()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                fetchDescriptorsUpdates(descriptors.orEmpty())
                Logger.d { "Descriptor update operation finished successfully" }
            } catch (e: Exception) {
                Logger.e(e) { "Descriptor update operation failed with exception" }
            }
        }
    }
}
