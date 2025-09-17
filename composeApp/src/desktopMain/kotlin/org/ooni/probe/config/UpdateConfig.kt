package org.ooni.probe.config

import org.ooni.probe.dependencies
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.Platform

object UpdateConfig {
    private const val MAC_URL =
        "https://github.com/aanorbel/oomplt-test/releases/latest/download/feed-mac.rss"
    private const val WINDOWS_URL =
        "https://github.com/aanorbel/oomplt-test/releases/latest/download/feed-windows.rss"

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
            ?: "1k8nI6WCqVly863R06ZaeSnxR/7oU5VAAnehA0Zfp/8="
    }
}
