package org.ooni.probe.shared

data class PlatformInfo(
    val buildName: String,
    val buildNumber: String,
    val platform: Platform,
    val osVersion: String,
    val model: String,
    val requestNotificationsPermission: Boolean,
    val knownBatteryState: Boolean = true,
    val knownNetworkType: Boolean = true,
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
