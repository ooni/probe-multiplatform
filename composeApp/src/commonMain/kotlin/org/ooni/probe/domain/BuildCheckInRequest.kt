package org.ooni.probe.domain

import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.buildSoftwareName
import org.ooni.engine.models.EnginePreferences
import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.TaskOrigin
import org.ooni.passport.models.CheckInRequest
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.shared.PlatformInfo

class BuildCheckInRequest(
    private val getEnginePreferences: suspend () -> EnginePreferences,
    private val platformInfo: PlatformInfo,
    private val getBatteryState: () -> BatteryState,
    private val networkTypeFinder: NetworkTypeFinder,
) {
    suspend operator fun invoke(taskOrigin: TaskOrigin): CheckInRequest {
        val preferences = getEnginePreferences()
        val networkType = networkTypeFinder()
        return CheckInRequest(
            runType = taskOrigin.runType,
            charging = isBatteryCharging(),
            onWifi = if (networkType !is NetworkType.Unknown) {
                networkType == NetworkType.Wifi
            } else {
                null
            },
            // The back-end does its own geoIP DB lookup
            probeCc = "ZZ",
            probeAsn = "AS0",
            softwareName = platformInfo.buildSoftwareName(taskOrigin),
            softwareVersion = platformInfo.buildName,
            webConnectivity = CheckInRequest.WebConnectivity(
                preferences.enabledWebCategories,
            ),
        )
    }

    private fun isBatteryCharging(): Boolean =
        when (getBatteryState()) {
            BatteryState.NotCharging -> false
            BatteryState.Charging,
            BatteryState.Unknown,
            -> true
        }

    private val TaskOrigin.runType
        get() = when (this) {
            TaskOrigin.AutoRun -> "timed"
            TaskOrigin.OoniRun -> "manual"
        }
}
