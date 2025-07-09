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
    val supportsInAppLanguage: Boolean = false,
    val canPullToRefresh: Boolean = false,
    val sentryDsn: String,
) {
    val version get() = "$buildName ($buildNumber)"
}

sealed interface Platform {
    data object Android : Platform

    data object Ios : Platform

    data class Desktop(
        val osName: String,
    ) : Platform {
        val os get() = when {
            osName.startsWith("Windows", ignoreCase = true) -> DesktopOS.Windows
            osName.startsWith("Mac", ignoreCase = true) -> DesktopOS.Mac
            osName.startsWith("Linux", ignoreCase = true) -> DesktopOS.Linux
            else -> DesktopOS.Other
        }
    }

    val engineName
        get() = when (this) {
            Android -> "android"
            Ios -> "ios"
            is Desktop -> "desktop"
        }

    val name
        get() = when (this) {
            Android -> "Android"
            Ios -> "iOS"
            is Desktop -> "Desktop ($osName)"
        }
}

enum class DesktopOS {
    Windows,
    Mac,
    Linux,
    Other,
}
