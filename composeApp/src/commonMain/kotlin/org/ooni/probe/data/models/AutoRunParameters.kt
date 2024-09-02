package org.ooni.probe.data.models

sealed interface AutoRunParameters {
    data object Disabled : AutoRunParameters

    data class Enabled(
        val wifiOnly: Boolean,
        val onlyWhileCharging: Boolean,
    ) : AutoRunParameters
}
