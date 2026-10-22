package org.ooni.passport

import org.ooni.probe.SharedBuildConfig
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.Platform

/**
 * Constructs the User-Agent header value sent with Passport HTTP requests, e.g.:
 * `ooniprobe,3.8.2,android,ooniprobe-engine,3.17.2,ooniprobe-engine_3.17.2,ooni-passport,0.1.8,protocol-version,1.2.3`.
 *
 * @param softwareName The application's base software name (e.g. `ooniprobe`).
 * @param platform The target platform ([Platform]).
 * @param protocolVersion Optional protocol version string.
 * @see <a href="https://github.com/ooni/probe-cli/blob/v3.31.0/internal/engine/experiment.go#L141">probe-cli engine name</a>
 */
fun buildUserAgent(
    softwareName: String,
    platform: Platform,
    protocolVersion: String? = null,
): String {
    val platformName = platform.toPlatformName()
    val base = "$softwareName,${SharedBuildConfig.VERSION_NAME},$platformName," +
        "ooniprobe-engine,${SharedBuildConfig.ENGINE_VERSION}," +
        "ooniprobe-engine_${SharedBuildConfig.ENGINE_VERSION}," +
        "ooni-passport,${SharedBuildConfig.PASSPORT_VERSION}"
    return if (!protocolVersion.isNullOrEmpty()) {
        "$base,protocol-version,$protocolVersion"
    } else {
        base
    }
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
