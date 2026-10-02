package org.ooni.probe.data.models

import androidx.compose.ui.text.intl.Locale
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.shared.languageRegionString

data class MeasurementWithUrl(
    val measurement: MeasurementModel,
    val url: UrlModel?,
) {
    val explorerUrl: String?
        get() {
            val urlBuilder = URLBuilder(OrganizationConfig.explorerUrl)
            if (measurement.uid != null && measurement.uid.value.isNotBlank()) {
                urlBuilder.appendPathSegments(listOf("m", measurement.uid.value))
            } else if (measurement.reportId != null) {
                urlBuilder.appendPathSegments(listOf("measurement", measurement.reportId.value))
                url?.url?.let {
                    urlBuilder.parameters.append("input", it)
                }
            } else {
                return null
            }
            return urlBuilder.build().toString()
        }

    val webViewUrl: String?
        get() {
            val explorerUrl = explorerUrl ?: return null
            val webViewUrl = URLBuilder(explorerUrl)
            webViewUrl.parameters.append("webview", "true")
            webViewUrl.parameters.append("language", Locale.current.languageRegionString)
            return webViewUrl.build().toString()
        }
}
