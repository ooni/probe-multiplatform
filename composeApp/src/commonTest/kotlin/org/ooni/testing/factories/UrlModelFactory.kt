package org.ooni.testing.factories

import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.UrlModel
import kotlin.uuid.Uuid

object UrlModelFactory {
    fun build(
        id: UrlModel.Id? = null,
        url: String = "https://ooni.org?${Uuid.random()}",
        category: WebConnectivityCategory = WebConnectivityCategory.MISC,
        countryCode: String? = null,
    ) = UrlModel(
        id = id,
        url = url,
        category = category,
        countryCode = countryCode,
    )
}
