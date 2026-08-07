package org.ooni.probe.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import kotlinx.coroutines.runBlocking
import org.ooni.probe.core.CliEngineConfig
import org.ooni.probe.core.CliGeoIp
import org.ooni.probe.core.CliGeoIpGateway
import org.ooni.probe.core.buildDesktopCliGeoIpGateway
import java.nio.file.Files

fun interface CliGeoIpGatewayFactory {
    fun create(runtime: CliRuntime): CliGeoIpGateway
}

internal object ProductionCliGeoIpGatewayFactory : CliGeoIpGatewayFactory {
    override fun create(runtime: CliRuntime): CliGeoIpGateway {
        Files.createDirectories(runtime.paths.dataDir)
        return buildDesktopCliGeoIpGateway(
            CliEngineConfig(
                databaseDir = runtime.paths.dataDir.toString(),
                baseFileDir = runtime.paths.dataDir.toString(),
                cacheDir = runtime.paths.cacheDir.toString(),
                ooniApiBaseUrl = OONI_API_BASE_URL,
                baseSoftwareName = "ooniprobe",
                softwareVersion = CliBuildConfig.VERSION_NAME,
                passportVersion = CliBuildConfig.VERSION_NAME,
                proxy = runtime.proxy,
                osName = System.getProperty("os.name") ?: "unknown",
                osVersion = System.getProperty("os.version") ?: "unknown",
            ),
        )
    }

    private const val OONI_API_BASE_URL = "https://api.ooni.io"
}

internal class GeoIpCommand(
    private val output: CliOutput,
    private val geoIpGatewayFactory: CliGeoIpGatewayFactory,
) : CliktCommand(name = "geoip") {
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()

    override fun run() {
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        val gateway = geoIpGatewayFactory.create(runtime)
        try {
            val geoIp = runBlocking { gateway.lookup() }
            emit(runtime, geoIp)
        } finally {
            gateway.close()
        }
    }

    private fun emit(runtime: CliRuntime, geoIp: CliGeoIp) {
        if (runtime.jsonOutput) {
            output.stdout(
                CliJson.obj(
                    "probe_ip" to CliJson.str(geoIp.ip),
                    "probe_asn" to CliJson.str(geoIp.asn),
                    "probe_network_name" to CliJson.str(geoIp.networkName),
                    "probe_cc" to CliJson.str(geoIp.countryCode),
                ),
            )
        } else {
            output.stdout("IP: ${geoIp.ip ?: "unknown"}")
            output.stdout("ASN: ${geoIp.asn ?: "unknown"}")
            output.stdout("Network name: ${geoIp.networkName ?: "unknown"}")
            output.stdout("Country: ${geoIp.countryCode ?: "unknown"}")
        }
    }
}
