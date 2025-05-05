package org.ooni.probe.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.domain.descriptors.FetchDescriptorsUpdates
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class BackgroundWorkManager(
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
    private val runBackgroundTaskProvider: () -> RunBackgroundTask,
    private val getDescriptorUpdateProvider: () -> FetchDescriptorsUpdates,
) {
    private var autoRunJob: Job? = null
    private var autoUpdateJob: Job? = null

    fun startSingleRun(spec: RunSpecification?) {
        CoroutineScope(coroutineContext).launch {
            runBackgroundTaskProvider()(spec).collect()
        }
    }

    fun startDescriptorsUpdate(descriptors: List<InstalledTestDescriptorModel>?) {
        CoroutineScope(coroutineContext).launch {
            getDescriptorUpdateProvider()(descriptors.orEmpty())
        }
    }

    fun configureAutoRun(params: AutoRunParameters) {
        autoRunJob?.cancel()

        if (params is AutoRunParameters.Enabled) {
            autoRunJob = CoroutineScope(coroutineContext + SupervisorJob()).launch {
                while (true) {
                    delay(AUTO_RUN_PERIOD)
                    startSingleRun(null)
                }
            }
        }
    }

    fun configureDescriptorAutoUpdate(): Boolean {
        autoUpdateJob?.cancel()
        autoUpdateJob = CoroutineScope(coroutineContext + SupervisorJob()).launch {
            while (true) {
                delay(AUTO_UPDATE_PERIOD)
                startDescriptorsUpdate(null)
            }
        }
        return true
    }

    fun cancelDescriptorAutoUpdate(): Boolean {
        autoUpdateJob?.cancel()
        autoUpdateJob = null
        return true
    }

    companion object {
        private val AUTO_RUN_PERIOD = 1.hours
        private val AUTO_UPDATE_PERIOD = 1.days
    }
}
