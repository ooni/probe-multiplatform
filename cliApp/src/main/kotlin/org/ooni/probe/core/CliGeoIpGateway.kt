package org.ooni.probe.core

/**
 * CLI-facing geolocation surface: resolves where the probe currently appears on the network (IP,
 * ASN, network name, country code).
 *
 * The desktop implementation talks to the engine's `oonimkall` session `geolocate` call directly,
 * so it never starts a measurement — it only reports the current probe location the same way the
 * engine does before running one.
 */
interface CliGeoIpGateway {
    suspend fun lookup(): CliGeoIp

    fun close()
}

data class CliGeoIp(
    val ip: String?,
    val asn: String?,
    val networkName: String?,
    val countryCode: String?,
)
