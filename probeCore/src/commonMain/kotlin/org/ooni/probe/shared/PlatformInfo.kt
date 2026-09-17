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
    val languageSupport: LanguageSupport = LanguageSupport.NONE,
    val hasDonations: Boolean = true,
    val canPullToRefresh: Boolean = false,
    val supportsRunAtStartup: Boolean = false,
    val sentryDsn: String,
    val sentryExtraTags: Map<String, String> = emptyMap(),
    val installerStore: String? = null,
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

/** How the app exposes language selection on a given platform. */
enum class LanguageSupport {
    /** No Language entry in Settings (e.g. Android below 13). */
    NONE,

    /** Language entry opens the OS language settings (Android 13+, iOS). */
    SYSTEM_SETTINGS,

    /** Language entry opens the in-app language picker (Desktop). */
    IN_APP,
}
