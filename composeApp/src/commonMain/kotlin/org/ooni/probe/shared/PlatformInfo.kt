package org.ooni.probe.shared

interface PlatformInfo {
    val version: String
    val platform: Platform
    val osVersion: String
    val model: String
    val needsToRequestNotificationsPermission: Boolean
}

enum class Platform {
    Android,
    Ios,
}
