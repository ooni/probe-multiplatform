package org.ooni.probe.config

enum class OptionalFeature {
    CrashReporting,
}

interface FlavorConfigInterface {
    val optionalFeatures: Set<OptionalFeature>
        get() = setOf(
            OptionalFeature.CrashReporting,
        )
    val isCrashReportingEnabled: Boolean
        get() = optionalFeatures.contains(OptionalFeature.CrashReporting)
}
