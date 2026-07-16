package org.ooni.passport

import org.ooni.probe.SharedBuildConfig
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.Platform

/**
 * User-Agent sent with Passport HTTP requests, e.g.
 * `ooni-passport-x.y.z; ooniprobe-x.y.z; android`.
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
