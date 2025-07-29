package org.ooni.probe.shared

import co.touchlab.kermit.Logger

class WinSparkleUpdateManager : UpdateManager {
    private external fun nativeInit(appcastUrl: String): Int

    private external fun nativeCheckForUpdates(showUI: Boolean)

    private external fun nativeSetAutomaticCheckEnabled(enabled: Boolean)

    private external fun nativeSetUpdateCheckInterval(hours: Int)

    private external fun nativeSetAppDetails(
        companyName: String,
        appName: String,
        appVersion: String,
    )

    private external fun nativeCleanup()

    companion object {
        init {
            try {
                System.loadLibrary("updatebridge")
            } catch (e: UnsatisfiedLinkError) {
                Logger.e("Failed to load updatebridge library: ", e)
            }
        }
    }

    override fun initialize(
        appcastUrl: String,
        publicKey: String?,
    ) {
        // Set app details from build config
        nativeSetAppDetails("OONI", "OONI Probe", "5.1.0")

        val result = nativeInit(appcastUrl)
        if (result != 0) {
            Logger.e("Failed to initialize WinSparkle updater: $result")
        }
    }

    override fun checkForUpdates(showUI: Boolean) {
        nativeCheckForUpdates(showUI)
    }

    override fun setAutomaticUpdatesEnabled(enabled: Boolean) {
        nativeSetAutomaticCheckEnabled(enabled)
    }

    override fun setUpdateCheckInterval(hours: Int) {
        nativeSetUpdateCheckInterval(hours)
    }

    override fun cleanup() {
        nativeCleanup()
    }
}
