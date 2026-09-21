package org.ooni.probe.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import oonimkall.Oonimkall
import oonimkall.SessionConfig

/** Builds the desktop/JVM CLI geoip gateway (direct `oonimkall` geolocate path). */
fun buildDesktopCliGeoIpGateway(config: CliEngineConfig): CliGeoIpGateway = DesktopCliGeoIpGateway(config)

private class DesktopCliGeoIpGateway(
    private val config: CliEngineConfig,
) : CliGeoIpGateway {
    // The oonimkall session is constructed lazily inside lookup() (never in the constructor) so a
    // close()-only path never dlopens the native library. Each lookup builds and closes its own
    // session, which is why close() has nothing to release.
    override suspend fun lookup(): CliGeoIp =
        withContext(Dispatchers.IO) {
            val session = Oonimkall.newSession(config.toSessionConfig())
            try {
                val results = session.geolocate(session.newContextWithTimeout(CONTEXT_TIMEOUT))
                CliGeoIp(
                    ip = results.getIP().nullIfBlank(),
                    asn = results.getASN().nullIfBlank(),
                    networkName = results.getOrg().nullIfBlank(),
                    countryCode = results.getCountry().nullIfBlank(),
                )
            } finally {
                runCatching { session.close() }
            }
        }

    override fun close() = Unit

    private fun CliEngineConfig.toSessionConfig(): SessionConfig =
        SessionConfig().also {
            it.softwareName = "$baseSoftwareName-$CLI_ENGINE_NAME"
            it.softwareVersion = softwareVersion

            it.assetsDir = "$baseFileDir/assets"
            geoipDbPath?.let { path -> it.geoipDB = path }
            it.stateDir = "$baseFileDir/state"
            it.tempDir = cacheDir
            it.tunnelDir = "$baseFileDir/tunnel"

            it.probeServicesURL = ooniApiBaseUrl
            it.proxy = proxy
            it.verbose = false
        }

    private fun String?.nullIfBlank(): String? = takeUnless { it.isNullOrBlank() }

    private companion object {
        private const val CONTEXT_TIMEOUT = -1L
    }
}
