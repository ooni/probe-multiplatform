package org.ooni.probe.core

import org.ooni.engine.DeleteAllResult
import org.ooni.engine.createDesktopSecureStorage
import org.ooni.probe.shared.Platform

/**
 * Builds the desktop/JVM CLI reset gateway, which clears the OONI-scoped secure storage during
 * `reset --force`.
 *
 * The platform [org.ooni.engine.SecureStorage] is constructed lazily (never in the constructor), so
 * building the gateway — or building it and only calling [CliResetGateway.close] — performs no native
 * keychain/secret access. Only [clearSecureStorage] touches the platform store.
 *
 * ## Why `deleteAll()` is safe here (scope decision)
 *
 * `deleteAll()` is provably scoped to the OONI app namespace on every desktop platform, so it can
 * never wipe unrelated keychain/secret entries. We therefore use `deleteAll()` directly rather than
 * hand-maintaining a credential-key list:
 *
 * - **macOS** — [org.ooni.engine.securestorage.MacOsSecureStorage.deleteAll] enumerates only a
 *   self-maintained key index (`"__${baseSoftwareName}_key_index__"`) stored under `service = appId`
 *   and populated exclusively by this class's own `write()`; it deletes each tracked key under the
 *   same `service = appId`. It never enumerates the system keychain, so it only ever removes keys
 *   OONI itself wrote.
 * - **Linux** — [org.ooni.engine.securestorage.LinuxSecureStorage.deleteAll] operates on a per-app
 *   libsecret schema (`"$appId.credentials"`) and clears only entries under that schema plus its own
 *   index entry.
 * - **Windows** — [org.ooni.engine.securestorage.WindowsSecureStorage.deleteAll] enumerates via
 *   `CredEnumerateW` filtered to the `baseSoftwareName/` target-name prefix and deletes only
 *   credentials carrying that OONI prefix.
 *
 * Both `appId` and `baseSoftwareName` here are the OONI software name ([CliEngineConfig.baseSoftwareName]),
 * so `deleteAll()` clears exactly the OONI-owned credential set and nothing else.
 */
fun buildDesktopCliResetGateway(config: CliEngineConfig): CliResetGateway = DesktopCliResetGateway(config)

private class DesktopCliResetGateway(
    config: CliEngineConfig,
) : CliResetGateway {
    private val desktopOS = Platform.Desktop(config.osName).os
    private val appId = config.baseSoftwareName

    // Lazy: constructing the gateway never dlopens the keychain/secret library; only a real
    // clearSecureStorage() call resolves the platform SecureStorage.
    private val secureStorage by lazy {
        createDesktopSecureStorage(desktopOS = desktopOS, appId = appId, baseSoftwareName = appId)
    }

    override suspend fun clearSecureStorage() {
        when (val result = secureStorage.deleteAll()) {
            is DeleteAllResult.DeletedCount -> Unit
            is DeleteAllResult.Error ->
                throw IllegalStateException(
                    "Failed to clear OONI secure storage: ${result.message ?: "unknown error"}",
                    result.cause,
                )
        }
    }

    override fun close() = Unit
}
