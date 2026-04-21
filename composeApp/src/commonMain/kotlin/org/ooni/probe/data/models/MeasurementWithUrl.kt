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
    val webViewUrl: String?
        get() {
            val webViewUrl = URLBuilder(OrganizationConfig.explorerUrl)
            if (measurement.uid != null && measurement.uid.value.isNotBlank()) {
                webViewUrl.appendPathSegments(listOf("m", measurement.uid.value))
            } else if (measurement.reportId != null) {
                webViewUrl.appendPathSegments(listOf("measurement", measurement.reportId.value))
                url?.url?.let {
                    webViewUrl.parameters.append("input", it)
                }
            } else {
                return null
            }
            webViewUrl.parameters.append("webview", "true")
            webViewUrl.parameters.append("language", Locale.current.languageRegionString)
            return webViewUrl.build().toString()
        }
}
