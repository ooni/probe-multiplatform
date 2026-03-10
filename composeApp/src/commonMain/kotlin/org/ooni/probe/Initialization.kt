package org.ooni.probe

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.PlatformInfo

// On App Start
fun initialization(dependencies: Dependencies) = initialization(dependencies, CoroutineScope(Dispatchers.Default))

fun initialization(
    dependencies: Dependencies,
    coroutineScope: CoroutineScope,
) {
    coroutineScope.launch {
        Logger.setMinSeverity(Severity.Verbose)
        Logger.addLogWriter(dependencies.crashMonitoring.logWriter)
        Logger.addLogWriter(dependencies.appLogger.logWriter)
        logAppStart(dependencies.platformInfo)
        dependencies.appLogger.writeLogsToFile()
    }
    coroutineScope.launch {
        if (dependencies.flavorConfig.isCrashReportingEnabled) {
            dependencies.crashMonitoring.setup()
        }
    }
    coroutineScope.launch {
        dependencies.bootstrapTestDescriptors()
        dependencies.bootstrapPreferences()
    }
    coroutineScope.launch {
        dependencies.fetchGeoIpDbUpdates()
    }
    coroutineScope.launch {
        dependencies.finishInProgressData()
        dependencies.deleteOldResults()
    }
}

private fun logAppStart(platformInfo: PlatformInfo) {
    with(platformInfo) {
        Logger.v(
            """
            ---APP START---
            Platform: $platform ($osVersion)"
            Version: $version
            Model: $model
            """.trimIndent(),
        )
    }
}
