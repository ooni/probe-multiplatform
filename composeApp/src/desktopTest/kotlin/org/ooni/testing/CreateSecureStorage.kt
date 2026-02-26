package org.ooni.testing

import org.ooni.engine.createDesktopSecureStorage
import org.ooni.engine.SecureStorage
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.shared.DesktopOS

internal actual fun createTestSecureStorage(): SecureStorage {
    val osName = System.getProperty("os.name")
    val desktopOS = when {
        osName.startsWith("Windows", ignoreCase = true) -> DesktopOS.Windows
        osName.startsWith("Mac", ignoreCase = true) -> DesktopOS.Mac
        osName.startsWith("Linux", ignoreCase = true) -> DesktopOS.Linux
        else -> DesktopOS.Other
    }
    return createDesktopSecureStorage(desktopOS, "${OrganizationConfig.appId}.testing", OrganizationConfig.baseSoftwareName)
}
