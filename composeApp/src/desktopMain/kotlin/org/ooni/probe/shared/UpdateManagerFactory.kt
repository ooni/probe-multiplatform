package org.ooni.probe.shared

fun createUpdateManager(platform: Platform): UpdateManager {
    if (!Distribution.current.supportsSelfUpdate) return NoOpUpdateManager()

    return when (platform) {
        is Platform.Desktop -> when (platform.os) {
            DesktopOS.Mac -> SparkleUpdateManager()
            DesktopOS.Windows -> WinSparkleUpdateManager()
            else -> NoOpUpdateManager()
        }
        else -> NoOpUpdateManager()
    }
}
