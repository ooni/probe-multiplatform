package org.ooni.engine

import org.ooni.engine.securestorage.LinuxSecureStorage
import org.ooni.engine.securestorage.MacOsSecureStorage
import org.ooni.engine.securestorage.WindowsSecureStorage
import org.ooni.probe.shared.DesktopOS

/**
 * Creates a [SecureStorage] instance appropriate for the given desktop OS:
 *
 * - **Linux**: [LinuxSecureStorage] — libsecret (GNOME Keyring / KDE Wallet)
 * - **Windows**: [WindowsSecureStorage] — Credential Manager (advapi32.dll)
 * - **macOS**: [MacOsSecureStorage] — Keychain (Security framework)
 */
fun createDesktopSecureStorage(
    desktopOS: DesktopOS,
    appId: String,
    baseSoftwareName: String,
): SecureStorage =
    when (desktopOS) {
        DesktopOS.Linux -> LinuxSecureStorage(appId, baseSoftwareName)
        DesktopOS.Windows -> WindowsSecureStorage(baseSoftwareName)
        DesktopOS.Mac -> MacOsSecureStorage(appId, baseSoftwareName)
        DesktopOS.Other -> throw UnsupportedOperationException(
            "Secure storage is not supported on this platform",
        )
    }
