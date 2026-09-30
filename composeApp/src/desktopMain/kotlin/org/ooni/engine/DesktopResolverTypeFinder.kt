package org.ooni.engine

import co.touchlab.kermit.Logger
import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.ResolverType
import org.ooni.shared.DesktopBridgeLoader

/**
 * Resolves the active network's [ResolverType] on macOS. Probes DNS transports for
 * [probeDomain] via Network.framework and maps them with the shared [ResolverTypeMapper].
 */
class DesktopResolverTypeFinder(
    private val networkTypeFinder: NetworkTypeFinder,
    private val probeDomain: String?,
    private val timeoutMillis: Long = 3000,
    private val probeDnsProtocols: ((host: String, timeoutMillis: Long) -> List<String>?)? = null,
    private val mapper: ResolverTypeMapper,
) : ResolverTypeFinder {
    private val logger = Logger.withTag("DesktopResolverTypeFinder")

    /** Native probe returning observed DNS protocols, or null on failure/timeout. */
    private external fun nativeProbeDnsProtocols(
        host: String,
        timeoutMillis: Long,
        log: (String) -> Unit,
    ): Array<String>?

    override fun invoke(): ResolverType {
        val networkType = networkTypeFinder()
        logger.d("networkType: ${networkType::class.simpleName}")

        if (networkType is NetworkType.NoInternet) {
            logger.d("No internet connection; skipping Private DNS probe")
            return mapper.resolverType(networkType, null)
        }

        return try {
            val dnsProtocols = readDnsProtocols()
            val resolverType = mapper.resolverType(networkType, dnsProtocols)
            logger.d("dnsProtocols: $dnsProtocols -> resolverType: ${resolverType.value}")
            resolverType
        } catch (e: Throwable) {
            logger.w("Error reading resolver type: ${e.message}")
            mapper.resolverType(networkType, null)
        }
    }

    /** Resolves [probeDomain] and returns observed DNS protocol/source names, or null on failure/timeout. */
    private fun readDnsProtocols(): List<String>? {
        val domain = probeDomain
        if (domain == null) {
            logger.d("No probe domain configured; cannot determine Private DNS state")
            return null
        }
        logger.d("Probing Private DNS using domain: $domain")
        return (probeDnsProtocols ?: ::loadAndProbeDnsProtocols)(domain, timeoutMillis)
    }

    private fun loadAndProbeDnsProtocols(
        host: String,
        timeoutMillis: Long,
    ): List<String>? = if (DesktopBridgeLoader.ensureLoaded()) nativeProbeDnsProtocols(host, timeoutMillis, logger::d)?.toList() else null
}
