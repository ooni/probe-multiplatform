package org.ooni.probe.config

import android.os.PowerManager

class AndroidBatteryOptimization(
    private val powerManager: PowerManager,
    private val packageName: String,
    private val requestCall: (() -> Unit) -> Unit,
) : BatteryOptimization {
    override val isSupported = true

    override val isIgnoring: Boolean
        get() = powerManager.isIgnoringBatteryOptimizations(packageName)

    override fun requestIgnore(onResponse: (Boolean) -> Unit) {
        requestCall {
            onResponse(isIgnoring)
        }
    }
}
