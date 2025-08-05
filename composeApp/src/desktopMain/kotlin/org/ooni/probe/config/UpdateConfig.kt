package org.ooni.probe.config

import org.ooni.probe.dependencies
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.Platform

object UpdateConfig {
    private const val MAC_URL =
        "https://s3api.lab.ambanumben.net/ooni/feed.rss"
    private const val WINDOWS_URL =
        "https://s3api.lab.ambanumben.net/ooni/feed-windows.rss"

    val URL = when (dependencies.platformInfo.platform) {
        is Platform.Desktop -> when (dependencies.platformInfo.platform.os) {
            DesktopOS.Mac -> MAC_URL
            DesktopOS.Windows -> WINDOWS_URL
            else -> ""
        }

        else -> ""
    }

    val PUBLIC_KEY by lazy {
        System.getProperty("desktopUpdatesPublicKey")
            ?: "NSSMAR1POATrcPOX+UGVPB58phK2XyVSyUEEX4IzCzU="
    }
}
