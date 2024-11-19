package org.ooni.probe.shared

interface PlatformInfo {
    val version: String
        get() = "$buildName ($buildNumber)"
    val buildName: String
    val buildNumber: String
    val platform: Platform
    val osVersion: String
    val model: String
    val needsToRequestNotificationsPermission: Boolean
    val sentryDsn: String
}

enum class Platform {
    Android,
    Ios,
}

val Platform.value
    get() =
        when (this) {
            Platform.Android -> "android"
            Platform.Ios -> "ios"
        }
