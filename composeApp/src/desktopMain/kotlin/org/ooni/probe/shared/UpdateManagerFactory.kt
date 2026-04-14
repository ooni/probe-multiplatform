package org.ooni.probe.shared

import org.ooni.probe.DesktopBuildConfig

fun createUpdateManager(platform: Platform): UpdateManager {
    if (DesktopBuildConfig.IS_APP_STORE) return NoOpUpdateManager()

    return when (platform) {
        is Platform.Desktop -> when (platform.os) {
            DesktopOS.Mac -> SparkleUpdateManager()
            DesktopOS.Windows -> WinSparkleUpdateManager()
            else -> NoOpUpdateManager()
        }
        else -> NoOpUpdateManager()
    }
}
