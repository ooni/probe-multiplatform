package org.ooni.testing.factories

import org.ooni.engine.models.NetworkType
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.UrlModel.Id

object NetworkModelFactory {
    fun build(
        id: NetworkModel.Id? = null,
        networkName: String? = "Vodafone",
        ip: String? = null,
        asn: String? = null,
        countryCode: String? = null,
        networkType: NetworkType? = null,
    ) = NetworkModel(
        id = id,
        networkName = networkName,
        ip = ip,
        asn = asn,
        countryCode = countryCode,
        networkType = networkType,
    )
}
