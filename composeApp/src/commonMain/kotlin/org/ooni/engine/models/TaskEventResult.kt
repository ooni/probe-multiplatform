package org.ooni.engine.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class TaskEventResult {
    @SerialName("key")
    var key: String? = null

    @SerialName("value")
    var value: Value? = null

    @Serializable
    class Value {
        @SerialName("key")
        var key: Double = 0.0

        @SerialName("log_level")
        var logLevel: String? = null

        @SerialName("message")
        var message: String? = null

        @SerialName("percentage")
        var percentage: Double = 0.0

        @SerialName("json_str")
        var jsonStr: String? = null

        @SerialName("idx")
        var idx: Int = 0

        @SerialName("report_id")
        var reportId: String? = null

        @SerialName("probe_ip")
        var probeIp: String? = null

        @SerialName("probe_asn")
        var probeAsn: String? = null

        @SerialName("probe_cc")
        var probeCc: String? = null

        @SerialName("probe_network_name")
        var probeNetworkName: String? = null

        @SerialName("downloaded_kb")
        var downloadedKb: Double = 0.0

        @SerialName("uploaded_kb")
        var uploadedKb: Double = 0.0

        @SerialName("input")
        var input: String? = null

        @SerialName("failure")
        var failure: String? = null

        @SerialName("orig_key")
        var origKey: String? = null
    }
}
