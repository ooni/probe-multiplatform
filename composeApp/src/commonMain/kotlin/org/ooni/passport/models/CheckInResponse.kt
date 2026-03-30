package org.ooni.passport.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.ooni.engine.models.TestType
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.UrlModel

@Serializable
data class CheckInResponse(
    @SerialName("conf") val conf: Conf?,
    @SerialName("tests") val tests: Tests?,
) {
    @Serializable
    data class Conf(
        @SerialName("features") val features: Map<String, Boolean>?,
    )

    @Serializable
    data class Tests(
        @SerialName("web_connectivity") val webConnectivity: Test?,
    )

    @Serializable
    data class Test(
        @SerialName("urls") val urls: List<Website>?,
    )

    @Serializable
    data class Website(
        @SerialName("category_code") val categoryCode: String?,
        @SerialName("country_code") val countryCode: String?,
        @SerialName("url") val url: String?,
    )

    val urls get() =
        tests
            ?.webConnectivity
            ?.urls
            ?.mapNotNull { it.toModel() }
            .orEmpty()

    private fun Website.toModel(): UrlModel? {
        return UrlModel(
            url = url ?: return null,
            countryCode = countryCode,
            category = WebConnectivityCategory.fromCode(categoryCode),
        )
    }

    val disabledTests get() =
        conf
            ?.features
            .orEmpty()
            .mapNotNull { (key, value) ->
                if (key.endsWith("_enabled") && !value) {
                    TestType.fromName(key.substringBefore("_enabled"))
                } else {
                    null
                }
            }
}
