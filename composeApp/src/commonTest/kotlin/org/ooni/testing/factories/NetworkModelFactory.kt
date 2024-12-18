package org.ooni.testing.factories

import org.ooni.engine.models.NetworkType
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.UrlModel.Id

object NetworkModelFactory {
    fun build(
        id: NetworkModel.Id? = null,
        networkName: String? = "Vodafone",
        asn: String? = null,
        countryCode: String? = null,
        networkType: NetworkType? = null,
    ) = NetworkModel(
        id = id,
        networkName = networkName,
        asn = asn,
        countryCode = countryCode,
        networkType = networkType,
    )
}
