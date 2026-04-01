package org.ooni.testing.factories

import org.ooni.engine.models.NetworkType
import org.ooni.probe.data.models.NetworkModel

object NetworkModelFactory {
    fun build(
        id: NetworkModel.Id? = null,
        networkName: String? = "Vodafone",
        asn: String? = null,
        countryCode: String? = null,
        networkType: NetworkType? = null,
    ) = NetworkModel(
        id = id,
        name = networkName,
        asn = asn,
        countryCode = countryCode,
        networkType = networkType,
    )
}
