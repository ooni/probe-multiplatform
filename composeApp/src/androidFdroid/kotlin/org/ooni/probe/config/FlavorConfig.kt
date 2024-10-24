package org.ooni.probe.config

class FlavorConfig : FlavorConfigInterface {
    override val optionalFeatures: Set<OptionalFeature>
        get() = emptySet()
    override val isCrashReportingEnabled: Boolean
        get() = optionalFeatures.contains(OptionalFeature.CrashReporting)
    override val isRemoteNotificationsEnabled: Boolean
        get() = optionalFeatures.contains(OptionalFeature.RemoteNotifications)
}
