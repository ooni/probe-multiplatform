package org.ooni.probe.shared

import co.touchlab.kermit.Logger

class SparkleUpdateManager : UpdateManager {
    private external fun nativeInit(
        appcastUrl: String,
        publicKey: String?,
    ): Int

    private external fun nativeCheckForUpdates(showUI: Boolean)

    private external fun nativeSetAutomaticCheckEnabled(enabled: Boolean)

    private external fun nativeSetUpdateCheckInterval(hours: Int)

    private external fun nativeCleanup()

    companion object {
        init {
            try {
                System.loadLibrary("updatebridge")
            } catch (e: UnsatisfiedLinkError) {
                Logger.e("Failed to load updatebridge library:", e)
            }
        }
    }

    override fun initialize(
        appcastUrl: String,
        publicKey: String?,
    ) {
        val result = nativeInit(appcastUrl, publicKey)
        if (result != 0) {
            Logger.e("Failed to initialize Sparkle updater: $result")
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
