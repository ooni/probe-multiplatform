package org.ooni.engine.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = NetworkTypeSerializer::class)
sealed interface NetworkType {
    val value: String

    data object VPN : NetworkType {
        override val value = "vpn"
    }

    data object Wifi : NetworkType {
        override val value = "wifi"
    }

    data object Mobile : NetworkType {
        override val value = "mobile"
    }

    data object NoInternet : NetworkType {
        override val value = "no_internet"
    }

    data class Unknown(
        override val value: String,
    ) : NetworkType

    companion object {
        fun fromValue(value: String) =
            when (value) {
                VPN.value -> VPN
                Wifi.value -> Wifi
                Mobile.value -> Mobile
                NoInternet.value -> NoInternet
                else -> Unknown(value)
            }
    }
}

object NetworkTypeSerializer : KSerializer<NetworkType> {
    override val descriptor = PrimitiveSerialDescriptor("NetworkType", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: NetworkType,
    ) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): NetworkType = NetworkType.fromValue(decoder.decodeString())
}
