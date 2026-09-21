package org.ooni.probe.data.models

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

    val unit: Unit
        get() =
            if (value < 1000) {
                Unit.KB
            } else if (value < 1_000_000) {
                Unit.MB
            } else {
                Unit.GB
            }

    enum class Unit {
        KB,
        MB,
        GB,
    }
}
