package org.ooni.probe.shared

actual fun createUpdateManager(platform: Platform): UpdateManager =
    when (platform) {
        is Platform.Desktop -> when (platform.os) {
            DesktopOS.Mac, DesktopOS.Windows -> SparkleUpdateManager(platform.os)
            else -> NoOpUpdateManager()
        }
        else -> NoOpUpdateManager()
    }
