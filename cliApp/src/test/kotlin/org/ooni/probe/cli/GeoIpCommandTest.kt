package org.ooni.probe.cli

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.ooni.probe.core.CliGeoIp
import org.ooni.probe.core.CliGeoIpGateway
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoIpCommandTest {
    private val runtime = CliRuntime(
        paths = CliPathLayout.create(ooniHome = Path.of("/tmp/ooni-cli-geoip-home"), tempDir = Path.of("/tmp")),
    )

    @Test
    fun humanOutputPrintsAllFields() {
        val factory = FakeCliGeoIpGatewayFactory(
            CliGeoIp(ip = "1.2.3.4", asn = "AS12345", networkName = "Example Networks", countryCode = "US"),
        )
        val result = runCli("geoip", geoIp = factory)

        assertEquals(0, result.code)
        assertEquals(
            listOf(
                "IP: 1.2.3.4",
                "ASN: AS12345",
                "Network name: Example Networks",
                "Country: US",
            ),
            result.stdout,
        )
        assertTrue(factory.gateway.closed)
    }

    @Test
    fun jsonOutputParsesAndCarriesProbeFields() {
        val factory = FakeCliGeoIpGatewayFactory(
            CliGeoIp(ip = "5.6.7.8", asn = "AS4242", networkName = "Test ISP", countryCode = "DE"),
        )
        val result = runCli("geoip", "--json", geoIp = factory)

        assertEquals(0, result.code)
        val obj = Json.parseToJsonElement(result.stdout.single()).jsonObject
        assertEquals("5.6.7.8", obj["probe_ip"]!!.jsonPrimitive.content)
        assertEquals("AS4242", obj["probe_asn"]!!.jsonPrimitive.content)
        assertEquals("Test ISP", obj["probe_network_name"]!!.jsonPrimitive.content)
        assertEquals("DE", obj["probe_cc"]!!.jsonPrimitive.content)
        assertTrue(factory.gateway.closed)
    }

    @Test
    fun extraArgumentFailsWithUsageErrorAndNoLookup() {
        val factory = FakeCliGeoIpGatewayFactory(
            CliGeoIp(ip = "1.1.1.1", asn = "AS1", networkName = "n", countryCode = "US"),
        )
        val result = runCli("geoip", "extra-arg", geoIp = factory)

        assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
        assertEquals(emptyList(), result.stdout)
        assertTrue(result.stderr.isNotEmpty())
        assertEquals(0, factory.creations)
        assertEquals(0, factory.gateway.lookups)
    }

    private fun runCli(
        vararg args: String,
        geoIp: FakeCliGeoIpGatewayFactory,
    ): GeoIpCliResult {
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()
        val code = OoniprobeCli(
            runtime = runtime,
            geoIpGatewayFactory = geoIp,
            input = { null },
        ).run(args.toList().toTypedArray(), stdout::add, stderr::add)
        return GeoIpCliResult(code, stdout, stderr)
    }

    private data class GeoIpCliResult(
        val code: Int,
        val stdout: List<String>,
        val stderr: List<String>,
    )
}

private class FakeCliGeoIpGatewayFactory(
    result: CliGeoIp,
    val gateway: FakeCliGeoIpGateway = FakeCliGeoIpGateway(result),
) : CliGeoIpGatewayFactory {
    var creations = 0

    override fun create(runtime: CliRuntime): CliGeoIpGateway {
        creations++
        return gateway
    }
}

private class FakeCliGeoIpGateway(
    private val result: CliGeoIp,
) : CliGeoIpGateway {
    var lookups = 0
    var closed = false

    override suspend fun lookup(): CliGeoIp {
        lookups++
        return result
    }

    override fun close() {
        closed = true
    }
}
