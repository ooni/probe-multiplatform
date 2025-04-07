package org.ooni.probe.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import kotlin.coroutines.CoroutineContext

class ObserveAndConfigureAutoUpdate(
    private val backgroundContext: CoroutineContext,
    private val listAllInstalledTestDescriptors: () -> Flow<List<InstalledTestDescriptorModel>>,
    private val configureDescriptorAutoUpdate: suspend () -> Boolean,
    private val cancelDescriptorAutoUpdate: suspend () -> Boolean,
    private val startDescriptorsUpdate: suspend (List<InstalledTestDescriptorModel>?) -> Unit,
) {
    operator fun invoke() =
        listAllInstalledTestDescriptors()
            .onEach {
                if (it.isNotEmpty()) {
                    configureDescriptorAutoUpdate()
                    startDescriptorsUpdate(null)
                } else {
                    cancelDescriptorAutoUpdate()
                }
            }
            .launchIn(CoroutineScope(backgroundContext))
}
