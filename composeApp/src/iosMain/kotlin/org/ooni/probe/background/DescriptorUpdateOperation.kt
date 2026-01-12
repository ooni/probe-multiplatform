package org.ooni.probe.background

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.ooni.probe.data.models.DescriptorItem
import platform.Foundation.NSOperation

class DescriptorUpdateOperation(
    private val descriptors: List<DescriptorItem>? = null,
    private val fetchDescriptorsUpdates: suspend (List<DescriptorItem>) -> Unit,
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
