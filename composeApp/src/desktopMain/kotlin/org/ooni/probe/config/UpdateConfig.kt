package org.ooni.probe.config

import org.ooni.probe.dependencies
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.Platform

class UpdateConfig {
    companion object {
        private const val MAC_URL =
            "https://gist.githubusercontent.com/aanorbel/1d8a887f2a912c8dd2c6687011824787/raw/036713641f41c099dbe64e087a37eb754aa70505/feed.xml"
        private const val WINDOWS_URL =
            "https://gist.githubusercontent.com/aanorbel/69f00f24e96f397703882afa2f184a5b/raw/94bcdb0b14762896d22e39d7fd22e6d05aecb96e/feed.rss"

        val URL = when (dependencies.platformInfo.platform) {
            is Platform.Desktop -> when (dependencies.platformInfo.platform.os) {
                DesktopOS.Mac -> MAC_URL
                DesktopOS.Windows -> WINDOWS_URL
                else -> ""
            }
            else -> ""
        }

        const val PUBLIC_KEY = "NSSMAR1POATrcPOX+UGVPB58phK2XyVSyUEEX4IzCzU="
    }
}
