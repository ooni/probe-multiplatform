package org.ooni.probe.shared

fun createUpdateManager(platform: Platform): UpdateManager =
    when (platform) {
        is Platform.Desktop -> when (platform.os) {
            DesktopOS.Windows -> WinSparkleUpdateManager()
            else -> NoOpUpdateManager()
        }
        else -> NoOpUpdateManager()
    }
