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
    val platformName = platform.toPlatformName()
    return "ooni-passport-${SharedBuildConfig.PASSPORT_VERSION}; " +
        "$softwareName-${SharedBuildConfig.VERSION_NAME}; " +
        platformName
}

/**
 * Constructs the User-Agent header value sent with Passport HTTP requests (`commonHeaders`), e.g.:
 * `ooniprobe,3.8.2,android,ooniprobe-engine,3.17.2,ooniprobe-engine_3.17.2`.
 *
 * @param softwareName The application's base software name (e.g. `ooniprobe`).
 * @param platform The target platform ([Platform]).
 * @see <a href="https://github.com/ooni/probe-cli/blob/v3.31.0/internal/engine/experiment.go#L141">probe-cli engine name</a>
 */
fun buildUserAgent(
    softwareName: String,
    platform: Platform,
): String {
    val platformName = platform.toPlatformName()
    return "$softwareName,${SharedBuildConfig.VERSION_NAME},$platformName," +
        "ooniprobe-engine,${SharedBuildConfig.ENGINE_VERSION}," +
        "ooniprobe-engine_${SharedBuildConfig.ENGINE_VERSION}"
}

private fun Platform.toPlatformName(): String =
    when (this) {
        Platform.Android -> "android"
        Platform.Ios -> "ios"
        is Platform.Desktop -> when (os) {
            DesktopOS.Mac -> "macos"
            DesktopOS.Windows -> "windows"
            DesktopOS.Linux -> "linux"
            DesktopOS.Other -> "desktop"
        }
    }
