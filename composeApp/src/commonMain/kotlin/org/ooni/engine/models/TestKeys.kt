package org.ooni.engine.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import org.ooni.probe.di.Dependencies

@Serializable
data class TestKeys(
    @SerialName("blocking") val blocking: String? = null,
    @SerialName("accessible") val accessible: String? = null,
    @SerialName("sent") val sent: List<String>? = null,
    @SerialName("received") val received: List<String>? = null,
    @SerialName("failure") val failure: String? = null,
    @SerialName("whatsapp_endpoints_status") val whatsappEndpointsStatus: String? = null,
    @SerialName("whatsapp_web_status") val whatsappWebStatus: String? = null,
    @SerialName("registration_server_status") val registrationServerStatus: String? = null,
    @SerialName("facebook_tcp_blocking") val facebookTcpBlocking: Boolean? = null,
    @SerialName("facebook_dns_blocking") val facebookDnsBlocking: Boolean? = null,
    @SerialName("telegram_http_blocking") val telegramHttpBlocking: Boolean? = null,
    @SerialName("telegram_tcp_blocking") val telegramTcpBlocking: Boolean? = null,
    @SerialName("telegram_web_status") val telegramWebStatus: String? = null,
    @SerialName("signal_backend_status") val signalBackendStatus: String? = null,
    @SerialName("signal_backend_failure") val signalBackendFailure: String? = null,
    @SerialName("protocol") val protocol: Int? = null,
    @SerialName("simple") val simple: Simple? = null,
    @SerialName("summary") val summary: Summary? = null,
    @SerialName("server") val server: Server? = null,
    @SerialName("tampering") val tampering: Tampering? = null,
    // Psiphon
    @SerialName("bootstrap_time") val bootstrapTime: Double? = null,
    // Tor
    @SerialName("dir_port_total") val dirPortTotal: Long? = null,
    @SerialName("dir_port_accessible") val dirPortAccessible: Long? = null,
    @SerialName("obfs4_total") val obfs4Total: Long? = null,
    @SerialName("obfs4_accessible") val obfs4Accessible: Long? = null,
    @SerialName("or_port_dirauth_total") val orPortDirauthTotal: Long? = null,
    @SerialName("or_port_dirauth_accessible") val orPortDirauthAccessible: Long? = null,
    @SerialName("or_port_total") val orPortTotal: Long? = null,
    @SerialName("or_port_accessible") val orPortAccessible: Long? = null,
) {
    @Serializable
    data class Summary(
        @SerialName("upload") val upload: Double? = null,
        @SerialName("download") val download: Double? = null,
        @SerialName("ping") val ping: Double? = null,
        @SerialName("max_rtt") val maxRtt: Double? = null,
        @SerialName("avg_rtt") val avgRtt: Double? = null,
        @SerialName("min_rtt") val minRtt: Double? = null,
        @SerialName("mss") val mss: Double? = null,
        @SerialName("retransmit_rate") val retransmitRate: Double? = null,
    )

    @Serializable
    data class Server(
        @SerialName("hostname") val hostname: String? = null,
        @SerialName("site") val site: String? = null,
    )

    // DASHSummary and NDT5Summary
    @Serializable
    class Simple(
        @SerialName("median_bitrate") val medianBitrate: Double? = null,
        @SerialName("min_playout_delay") val minPlayoutDelay: Double? = null,
    )

    @Serializable(with = TamperingSerializer::class)
    data class Tampering(
        val value: Boolean = false,
    )

    @Serializable
    data class TamperingKeys(
        @SerialName("header_field_name")
        val headerFieldName: Boolean = false,
        @SerialName("header_field_number")
        val headerFieldNumber: Boolean = false,
        @SerialName("header_field_value")
        val headerFieldValue: Boolean = false,
        @SerialName("header_name_capitalization")
        val headerNameCapitalization: Boolean = false,
        @SerialName("request_line_capitalization")
        val requestLineCapitalization: Boolean = false,
        @SerialName("total")
        val total: Boolean = false,
    ) {
        val value
            get() = headerFieldName || headerFieldNumber || headerFieldValue ||
                headerNameCapitalization || requestLineCapitalization || total
    }

    companion object {
        const val BLOCKED_VALUE = "BLOCKED"
    }
}

object TamperingSerializer : KSerializer<TestKeys.Tampering> {
    override val descriptor = PolymorphicSerializer(TestKeys.Tampering::class).descriptor

    override fun deserialize(decoder: Decoder): TestKeys.Tampering =
        when (val element = (decoder as JsonDecoder).decodeJsonElement()) {
            is JsonPrimitive ->
                TestKeys.Tampering(element.booleanOrNull == true)

            is JsonObject -> {
                val keys = Dependencies.buildJson()
                    .decodeFromJsonElement<TestKeys.TamperingKeys>(element)
                TestKeys.Tampering(keys.value)
            }

            else -> throw SerializationException("Could not deserialize TestKeys.Tampering")
        }

    override fun serialize(
        encoder: Encoder,
        value: TestKeys.Tampering,
    ) {
        encoder.encodeBoolean(value.value)
    }
}
