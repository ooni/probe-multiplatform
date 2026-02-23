package org.ooni.engine

/**
 * Desktop implementation of [SecureStorage] that delegates to a platform-specific
 * backend based on the detected operating system:
 *
 * - **Linux**: [LinuxSecureStorage] — libsecret (GNOME Keyring / KDE Wallet)
 * - **Windows**: [WindowsSecureStorage] — Credential Manager (advapi32.dll)
 * - **macOS**: [MacOsSecureStorage] — Keychain (Security framework)
 */
class DesktopSecureStorage : SecureStorage {
    private val delegate: SecureStorage by lazy {
        val osName = System.getProperty("os.name").lowercase()
        when {
            osName.contains("linux") -> LinuxSecureStorage()
            osName.contains("win") -> WindowsSecureStorage()
            osName.contains("mac") || osName.contains("darwin") -> MacOsSecureStorage()
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
