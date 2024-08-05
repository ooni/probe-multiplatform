package org.ooni.engine.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.ooni.engine.models.serializers.InstantSerializer

@Serializable
data class MeasurementResult(
    @SerialName("probe_asn")
    val probeAsn: String? = null,
    @SerialName("probe_cc")
    val probeCountryCode: String? = null,
    @SerialName("test_start_time")
    @Serializable(with = InstantSerializer::class)
    val testStartTime: Instant? = null,
    @SerialName("measurement_start_time")
    @Serializable(with = InstantSerializer::class)
    val measurementStartTime: Instant? = null,
    @SerialName("test_runtime")
    val testRuntime: Double? = null,
    @SerialName("probe_ip")
    val probeIp: String? = null,
    @SerialName("report_id")
    val reportId: String? = null,
    @SerialName("input")
    val input: String? = null,
    /*
     Field `test_keys` is ignored because we're not planning on storing the measurement results
     as structured data.
     */
)
