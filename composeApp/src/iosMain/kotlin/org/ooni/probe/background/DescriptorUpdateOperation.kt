package org.ooni.probe.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.repositories.TestDescriptorRepository
import org.ooni.probe.domain.descriptors.FetchDescriptorsUpdates
import platform.Foundation.NSOperation

class DescriptorUpdateOperation(
    private val descriptors: List<InstalledTestDescriptorModel>? = null,
    private val testDescriptorRepository: TestDescriptorRepository? = null,
    private val fetchDescriptorsUpdates: FetchDescriptorsUpdates,
) : NSOperation() {
    init {
        require(descriptors != null || testDescriptorRepository != null) {
            "Either descriptors or testDescriptorRepository must be provided"
        }
    }

    override fun main() {
        super.main()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            fetchDescriptorsUpdates.invoke(
                descriptors ?: testDescriptorRepository!!.listLatest().first(),
            )
        }
    }
}
