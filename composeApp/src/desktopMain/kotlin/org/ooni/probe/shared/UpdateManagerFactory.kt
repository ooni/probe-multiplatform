package org.ooni.probe.shared

actual fun createUpdateManager(platform: Platform): UpdateManager =
    when (platform) {
        is Platform.Desktop -> when (platform.os) {
            DesktopOS.Mac -> MacSparkleUpdateManager(platform.os)
             DesktopOS.Windows -> WinSparkleUpdateManager(platform.os)
            else -> NoOpUpdateManager()
        }
        else -> NoOpUpdateManager()
    }
