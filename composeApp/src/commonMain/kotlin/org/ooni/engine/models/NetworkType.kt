package org.ooni.engine.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = NetworkTypeSerializer::class)
enum class NetworkType(val value: String) {
    VPN("vpn"),
    Wifi("wifi"),
    Mobile("mobile"),
    NoInternet("no_internet"),
}

object NetworkTypeSerializer : KSerializer<NetworkType> {
    override val descriptor =
        PrimitiveSerialDescriptor("NetworkType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NetworkType) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): NetworkType {
        val string = decoder.decodeString()
        return NetworkType.entries.firstOrNull { it.value == string } ?: NetworkType.NoInternet
    }
}
