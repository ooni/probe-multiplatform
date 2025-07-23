package org.ooni.probe.config

interface BatteryOptimization {
    val isSupported: Boolean get() = false

    val isIgnoring: Boolean
        get() = throw IllegalStateException("Battery Optimization not supported")

    fun requestIgnore(onResponse: (Boolean) -> Unit = {}): Unit = throw IllegalStateException("Battery Optimization not supported")
}
