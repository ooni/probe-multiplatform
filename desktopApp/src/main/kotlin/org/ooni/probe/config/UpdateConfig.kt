package org.ooni.probe.config

import org.ooni.probe.dependencies
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.Platform

object UpdateConfig {
    const val CHECK_INTERVAL_HOURS = 6

    private const val GITHUB_MAC_URL =
        "https://github.com/ooni/probe-multiplatform/releases/latest/download/feed-mac.rss"
    private const val GITHUB_WINDOWS_URL =
        "https://github.com/ooni/probe-multiplatform/releases/latest/download/feed-windows.rss"
    private const val MIRROR_BASE_URL =
        "https://s3.eu-central-1.amazonaws.com/software-update-builds/"
    private const val MIRROR_MAC_URL = MIRROR_BASE_URL + "latest/feed-mac.rss"
    private const val MIRROR_WINDOWS_URL = MIRROR_BASE_URL + "latest/feed-windows.rss"

    /**
     * Ordered appcast feed URLs for the current platform. The first reachable host is used;
     * later entries are fallbacks for networks where GitHub is throttled or unreachable.
     */
    val URLS: List<String> = when (val platform = dependencies.platformInfo.platform) {
        is Platform.Desktop -> when (platform.os) {
            DesktopOS.Mac -> listOf(GITHUB_MAC_URL, MIRROR_MAC_URL)
            DesktopOS.Windows -> listOf(GITHUB_WINDOWS_URL, MIRROR_WINDOWS_URL)
            else -> emptyList()
        }

        else -> emptyList()
    }

    /** Whether [url] is served by the S3 mirror rather than GitHub. */
    fun isMirrorUrl(url: String): Boolean = url.startsWith(MIRROR_BASE_URL)

    val PUBLIC_KEY by lazy {
        System.getProperty("desktopUpdatesPublicKey")
            ?: "p1lTWmqHCTBhhCEtLT7sf/5pwS21mV3ZrvUudGnECLo="
    }
}
