package org.ooni.probe.domain

import androidx.annotation.VisibleForTesting
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.engine.models.NetworkType
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.shared.monitoring.Instrumentation
import org.ooni.probe.shared.monitoring.reportTransaction

class CheckAutoRunConstraints(
    private val getAutoRunSettings: suspend () -> Flow<AutoRunParameters>,
    private val getNetworkType: () -> NetworkType,
    private val getBatteryState: () -> BatteryState,
    private val knownNetworkType: Boolean,
    private val knownBatteryState: Boolean,
    private val countResultsMissingUpload: () -> Flow<Long>,
) {
    suspend operator fun invoke(): Boolean {
        val autoRunParameters = getAutoRunSettings().first()

        if (autoRunParameters !is AutoRunParameters.Enabled) {
            Logger.i("Not starting auto-run because auto-run is not enabled")
            Instrumentation.reportTransaction(
                "CheckAutoRunConstraints.Failed",
                data = mapOf("reason" to "auto-run is not enabled"),
            )
            return false
        }

        if (getNetworkType() == NetworkType.VPN) {
            Logger.i("Not starting auto-run because VPN is enabled")
            Instrumentation.reportTransaction(
                "CheckAutoRunConstraints.Failed",
                data = mapOf("reason" to "VPN is enabled"),
            )
            return false
        }

        if (knownNetworkType &&
            autoRunParameters.wifiOnly &&
            getNetworkType() != NetworkType.Wifi
        ) {
            Logger.i("Not starting auto-run because of Wi-Fi constraint")
            Instrumentation.reportTransaction(
                "CheckAutoRunConstraints.Failed",
                data = mapOf("reason" to "Wi-Fi constraint"),
            )
            return false
        }

        if (knownBatteryState &&
            autoRunParameters.onlyWhileCharging &&
            getBatteryState() == BatteryState.NotCharging
        ) {
            Logger.i("Not starting auto-run because of battery charging constraint")
            Instrumentation.reportTransaction(
                "CheckAutoRunConstraints.Failed",
                data = mapOf("reason" to "battery charging constraint"),
            )
            return false
        }

        val count = countResultsMissingUpload().first()
        if (count >= NOT_UPLOADED_LIMIT) {
            Logger.w(
                "Skipping auto-run due to not uploaded limit",
                SkipAutoRunException("Results missing upload: $count (limit=$NOT_UPLOADED_LIMIT)"),
            )
            Instrumentation.reportTransaction(
                "CheckAutoRunConstraints.Failed",
                data = mapOf("reason" to "not uploaded limit"),
            )
            return false
        }

        return true
    }

    companion object {
        @VisibleForTesting
        const val NOT_UPLOADED_LIMIT = 50
    }
}

class SkipAutoRunException(
    message: String,
) : Exception(message)
