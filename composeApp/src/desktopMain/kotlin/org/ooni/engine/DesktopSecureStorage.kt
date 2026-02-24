package org.ooni.engine

import org.ooni.engine.securestorage.LinuxSecureStorage
import org.ooni.engine.securestorage.MacOsSecureStorage
import org.ooni.engine.securestorage.WindowsSecureStorage

/**
 * Desktop implementation of [SecureStorage] that delegates to a platform-specific
 * backend based on the detected operating system:
 *
 * - **Linux**: [org.ooni.engine.securestorage.LinuxSecureStorage] — libsecret (GNOME Keyring / KDE Wallet)
 * - **Windows**: [org.ooni.engine.securestorage.WindowsSecureStorage] — Credential Manager (advapi32.dll)
 * - **macOS**: [org.ooni.engine.securestorage.MacOsSecureStorage] — Keychain (Security framework)
 */
class DesktopSecureStorage(
    private val appId: String,
    private val baseSoftwareName: String,
) : SecureStorage {
    private val delegate: SecureStorage by lazy {
        val osName = System.getProperty("os.name").lowercase()
        when {
            osName.contains("linux") -> LinuxSecureStorage(appId, baseSoftwareName)
            osName.contains("win") -> WindowsSecureStorage(baseSoftwareName)
            osName.contains("mac") || osName.contains("darwin") -> MacOsSecureStorage(appId, baseSoftwareName)
            else -> throw UnsupportedOperationException(
                "Secure storage is not supported on this platform: $osName",
            )
        }
    }

    override suspend fun read(key: String): String? = delegate.read(key)

    override suspend fun write(
        key: String,
        value: String,
    ): Boolean = delegate.write(key, value)

    override suspend fun exists(key: String): Boolean = delegate.exists(key)

    override suspend fun delete(key: String): Boolean = delegate.delete(key)

    override suspend fun list(): List<String> = delegate.list()

    override suspend fun deleteAll(): Boolean = delegate.deleteAll()
}
