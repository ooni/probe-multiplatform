package org.ooni.engine

import kotlinx.serialization.Serializable

@Serializable
class EventResult {
    var key: String? = null
    var value: Value? = null

    @Serializable
    class Value {
        var key: Double = 0.0
        var log_level: String? = null
        var message: String? = null
        var percentage: Double = 0.0
        var json_str: String? = null
        var idx: Int = 0
        var report_id: String? = null
        var probe_ip: String? = null
        var probe_asn: String? = null
        var probe_cc: String? = null
        var probe_network_name: String? = null
        var downloaded_kb: Double = 0.0
        var uploaded_kb: Double = 0.0
        var input: String? = null
        var failure: String? = null
        var orig_key: String? = null
    }
}
