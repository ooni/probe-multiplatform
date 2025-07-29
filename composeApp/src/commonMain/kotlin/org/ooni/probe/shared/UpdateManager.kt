package org.ooni.probe.shared

interface UpdateManager {
    fun initialize(
        appcastUrl: String,
        publicKey: String? = null,
    )

    fun checkForUpdates(showUI: Boolean = false)

    fun setAutomaticUpdatesEnabled(enabled: Boolean)

    fun setUpdateCheckInterval(hours: Int)

    fun cleanup()
}

expect fun createUpdateManager(platform: Platform): UpdateManager

class NoOpUpdateManager : UpdateManager {
    override fun initialize(
        appcastUrl: String,
        publicKey: String?,
    ) {}

    override fun checkForUpdates(showUI: Boolean) {}

    override fun setAutomaticUpdatesEnabled(enabled: Boolean) {}

    override fun setUpdateCheckInterval(hours: Int) {}

    override fun cleanup() {}
}
