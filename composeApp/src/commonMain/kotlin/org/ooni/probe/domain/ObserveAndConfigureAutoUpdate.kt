package org.ooni.probe.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.data.models.Descriptor
import kotlin.coroutines.CoroutineContext

class ObserveAndConfigureAutoUpdate(
    private val backgroundContext: CoroutineContext,
    private val listAllInstalledTestDescriptors: () -> Flow<List<Descriptor>>,
    private val configureDescriptorAutoUpdate: suspend () -> Boolean,
    private val cancelDescriptorAutoUpdate: suspend () -> Boolean,
    private val startDescriptorsUpdate: suspend (List<Descriptor>?) -> Unit,
) {
    operator fun invoke() =
        listAllInstalledTestDescriptors()
            .map { it.isNotEmpty() }
            .distinctUntilChanged()
            .onEach { enabled ->
                if (enabled) {
                    configureDescriptorAutoUpdate()
                    startDescriptorsUpdate(null)
                } else {
                    cancelDescriptorAutoUpdate()
                }
            }.launchIn(CoroutineScope(backgroundContext))
}
