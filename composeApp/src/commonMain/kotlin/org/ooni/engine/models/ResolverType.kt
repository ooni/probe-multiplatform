package org.ooni.engine.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Type of DNS resolver configured or used on the device.
 * Possible values in OONI annotations:
 * - "private_dns"
 * - "vpn"
 * - "system"
 * - "unknown"
 */
@Serializable(with = ResolverTypeSerializer::class)
sealed interface ResolverType {
    val value: String

    data object PrivateDns : ResolverType {
        override val value = "private_dns"
    }

    data object VPN : ResolverType {
        override val value = "vpn"
    }

    data object System : ResolverType {
        override val value = "system"
    }

    data object Unknown : ResolverType {
        override val value = "unknown"
    }

    companion object {
        fun fromValue(value: String): ResolverType =
            when (value) {
                PrivateDns.value -> PrivateDns
                VPN.value -> VPN
                System.value -> System
                else -> Unknown
            }

        fun from(
            isPrivateDnsActive: Boolean?,
            networkType: NetworkType,
        ): ResolverType =
            when {
                isPrivateDnsActive == true -> PrivateDns
                networkType is NetworkType.VPN -> VPN
                isPrivateDnsActive == false -> System
                else -> Unknown
            }
    }
}

object ResolverTypeSerializer : KSerializer<ResolverType> {
    override val descriptor = PrimitiveSerialDescriptor("ResolverType", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ResolverType,
    ) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ResolverType = ResolverType.fromValue(decoder.decodeString())
}
