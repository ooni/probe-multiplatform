package org.ooni.probe.config

enum class OptionalFeature {
    CrashReporting,
    RemoteNotifications,
}

interface FlavorConfigInterface {
    val optionalFeatures: Set<OptionalFeature>
        get() = setOf(
            OptionalFeature.CrashReporting,
            OptionalFeature.RemoteNotifications,
        )
    val isCrashReportingEnabled: Boolean
        get() = optionalFeatures.contains(OptionalFeature.CrashReporting)
    val isRemoteNotificationsEnabled: Boolean
        get() = optionalFeatures.contains(OptionalFeature.RemoteNotifications)
}
