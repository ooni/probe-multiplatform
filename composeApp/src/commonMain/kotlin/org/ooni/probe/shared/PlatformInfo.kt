package org.ooni.probe.shared

data class PlatformInfo(
    val buildName: String,
    val buildNumber: String,
    val platform: Platform,
    val osVersion: String,
    val model: String,
    val needsToRequestNotificationsPermission: Boolean,
    val supportsNotificationSettings: Boolean = true,
    val knownBatteryState: Boolean = true,
    val sentryDsn: String,
) {
    val version get() = "$buildName ($buildNumber)"
}

enum class Platform {
    Android,
    Ios,
    Desktop,
    ;

    val value
        get() = when (this) {
            Android -> "android"
            Ios -> "ios"
            Desktop -> "desktop"
        }
}
