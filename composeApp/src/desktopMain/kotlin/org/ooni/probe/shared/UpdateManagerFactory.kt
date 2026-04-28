package org.ooni.probe.shared

import co.touchlab.kermit.Logger

fun createUpdateManager(platform: Platform): UpdateManager {
    if (!Distribution.current.supportsSelfUpdate) return NoOpUpdateManager()

    return when (platform) {
        is Platform.Desktop -> when (platform.os) {
            DesktopOS.Mac -> try {
                SparkleUpdateManager()
            } catch (e: Throwable) {
                Logger.w("Failed to create SparkleUpdateManager, updates disabled", e)
                NoOpUpdateManager()
            }
            DesktopOS.Windows -> try {
                WinSparkleUpdateManager()
            } catch (e: Throwable) {
                Logger.w("Failed to create WinSparkleUpdateManager, updates disabled", e)
                NoOpUpdateManager()
            }
            else -> NoOpUpdateManager()
        }
        else -> NoOpUpdateManager()
    }
}
