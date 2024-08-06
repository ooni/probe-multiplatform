package org.ooni.testing.factories

import org.ooni.probe.data.models.UrlModel

object UrlModelFactory {
    fun build(
        id: UrlModel.Id? = null,
        url: String = "https://ooni.org",
        categoryCode: String? = null,
        countryCode: String? = null,
    ) = UrlModel(
        id = id,
        url = url,
        categoryCode = categoryCode,
        countryCode = countryCode,
    )
}
