package org.ooni.probe.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.data.models.AutoRunParameters

class ObserveAndConfigureAutoRun(
    private val backgroundDispatcher: CoroutineDispatcher,
    private val configureAutoRun: suspend (AutoRunParameters) -> Unit,
    private val getAutoRunSettings: suspend () -> Flow<AutoRunParameters>,
) {
    suspend operator fun invoke() =
        getAutoRunSettings()
            .onEach { configureAutoRun(it) }
            .launchIn(CoroutineScope(backgroundDispatcher))
}
