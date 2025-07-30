package org.ooni.probe.shared

actual fun createUpdateManager(platform: Platform): UpdateManager =
    when (platform) {
        is Platform.Desktop -> when (platform.os) {
            DesktopOS.Mac -> SparkleUpdateManager(platform.os)
            DesktopOS.Windows -> WinSparkleUpdateManager()
            else -> NoOpUpdateManager()
        }
        else -> NoOpUpdateManager()
    }
