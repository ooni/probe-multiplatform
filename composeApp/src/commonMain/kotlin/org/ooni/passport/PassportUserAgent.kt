package org.ooni.passport

import org.ooni.probe.SharedBuildConfig
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.Platform

/**
 * User-Agent sent with Passport HTTP requests, e.g.
 * `ooni-passport-0.1.4-beta; ooniprobe-6.1.1; android`.
 *
 * [softwareName] is the flavor's base software name (e.g. `ooniprobe`), passed in because it lives in
 * a flavor source set and is not visible from commonMain.
 */
fun passportUserAgent(
    platform: Platform,
    softwareName: String,
): String {
    val platformName = when (platform) {
        Platform.Android -> "android"
        Platform.Ios -> "ios"
        is Platform.Desktop -> when (platform.os) {
            DesktopOS.Mac -> "macos"
            DesktopOS.Windows -> "windows"
            DesktopOS.Linux -> "linux"
            DesktopOS.Other -> "desktop"
        }
    }
    return "ooni-passport-${SharedBuildConfig.PASSPORT_VERSION}; " +
        "$softwareName-${SharedBuildConfig.VERSION_NAME}; " +
        platformName
}
