package org.ooni.probe.data.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.ooni.engine.models.NetworkType

@Serializable
data class NetworkModel(
    @SerialName("id") val id: Id? = null,
    @SerialName("network_name") val networkName: String?,
    @SerialName("asn") val asn: String?,
    @SerialName("country_code") val countryCode: String?,
    @SerialName("networkType") val networkType: NetworkType?,
) {
    @Serializable(with = NetworkModelIdSerializer::class)
    data class Id(
        val value: Long,
    )

    fun isValid() = asn != "AS0" && !countryCode.equals("ZZ", ignoreCase = true)
}

object NetworkModelIdSerializer : KSerializer<NetworkModel.Id> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("org.ooni.probe.data.models.NetworkModel.Id", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): NetworkModel.Id = NetworkModel.Id(decoder.decodeLong())

    override fun serialize(
        encoder: Encoder,
        value: NetworkModel.Id,
    ) {
        encoder.encodeLong(value.value)
    }
}
