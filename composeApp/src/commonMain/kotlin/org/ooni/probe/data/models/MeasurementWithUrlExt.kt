package org.ooni.probe.data.models

import androidx.compose.ui.text.intl.Locale
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.shared.languageRegionString

val MeasurementWithUrl.webViewUrl: String?
    get() {
        val webViewUrl = URLBuilder(OrganizationConfig.explorerUrl)
        val measurementUid = measurement.uid
        val measurementReportId = measurement.reportId
        if (measurementUid != null && measurementUid.value.isNotBlank()) {
            webViewUrl.appendPathSegments(listOf("m", measurementUid.value))
        } else if (measurementReportId != null) {
            webViewUrl.appendPathSegments(listOf("measurement", measurementReportId.value))
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
