package org.ooni.probe.shared

interface PlatformInfo {
    val version: String
    val platform: Platform
    val osVersion: String
    val model: String
}

enum class Platform {
    Android,
    Ios,
}
