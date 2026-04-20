package org.ooni.probe.config

import android.os.PowerManager
import androidx.annotation.VisibleForTesting

class AndroidBatteryOptimization(
    private val powerManager: PowerManager,
    private val packageName: String,
    private val requestCall: (() -> Unit) -> Unit,
) : BatteryOptimization {
    override val isSupported get() = Companion.isSupported

    override val isIgnoring: Boolean
        get() = !this@AndroidBatteryOptimization.isSupported || powerManager.isIgnoringBatteryOptimizations(packageName)

    override fun requestIgnore(onResponse: (Boolean) -> Unit) {
        requestCall {
            onResponse(isIgnoring)
        }
    }

    companion object {
        @VisibleForTesting
        var isSupported = true
    }
}
