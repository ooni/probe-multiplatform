package org.ooni.probe.data.models

import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_Gbps
import ooniprobe.composeapp.generated.resources.TestResults_Kbps
import ooniprobe.composeapp.generated.resources.TestResults_Mbps
import org.jetbrains.annotations.VisibleForTesting
import org.ooni.probe.shared.withFractionalDigits

// We assume there is no Tbit/s (for now!)
data class ScaledValue(
    val value: Double,
) {
    val scaledValue: String
        get() = (
            if (value < 1000) {
                value
            } else if (value < 1_000_1000) {
                value / 1000
            } else {
                value / 1_000_000
            }
        ).withFractionalDigits()

    @get:VisibleForTesting
    val unit: Unit
        get() =
            if (value < 1000) {
                Unit.KB
            } else if (value < 1_000_000) {
                Unit.MB
            } else {
                Unit.GB
            }

    val unitStringId
        get() = when (unit) {
            Unit.KB -> Res.string.TestResults_Kbps
            Unit.MB -> Res.string.TestResults_Mbps
            Unit.GB -> Res.string.TestResults_Gbps
        }

    enum class Unit {
        KB,
        MB,
        GB,
    }
}
