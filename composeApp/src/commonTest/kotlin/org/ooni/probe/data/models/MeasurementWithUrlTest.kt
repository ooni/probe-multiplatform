package org.ooni.probe.data.models

import androidx.compose.ui.text.intl.Locale
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.shared.languageRegionString
import org.ooni.testing.factories.MeasurementModelFactory
import org.ooni.testing.factories.UrlModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MeasurementWithUrlTest {
    @Test
    fun withoutUidButWithReportId() {
        assertEquals(
            "${OrganizationConfig.explorerUrl}/measurement/REPORT?webview=true&language=${Locale.current.languageRegionString}",
            MeasurementWithUrl(
                measurement = MeasurementModelFactory.build(
                    uid = null,
                    reportId = MeasurementModel.ReportId("REPORT"),
                ),
                url = null,
            ).webViewUrl,
        )
        assertEquals(
            "${OrganizationConfig.explorerUrl}/measurement/REPORT?input=https%3A%2F%2Fexample.org&webview=true&language=${Locale.current.languageRegionString}",
            MeasurementWithUrl(
                measurement = MeasurementModelFactory.build(
                    uid = null,
                    reportId = MeasurementModel.ReportId("REPORT"),
                ),
                url = UrlModelFactory.build(url = "https://example.org"),
            ).webViewUrl,
        )
    }

    @Test
    fun withUid() {
        assertEquals(
            "${OrganizationConfig.explorerUrl}/m/MUID?webview=true&language=${Locale.current.languageRegionString}",
            MeasurementWithUrl(
                measurement = MeasurementModelFactory.build(
                    uid = MeasurementModel.Uid("MUID"),
                    reportId = MeasurementModel.ReportId("REPORT"),
                ),
                url = null,
            ).webViewUrl,
        )
        assertEquals(
            "${OrganizationConfig.explorerUrl}/m/MUID?webview=true&language=${Locale.current.languageRegionString}",
            MeasurementWithUrl(
                measurement = MeasurementModelFactory.build(
                    uid = MeasurementModel.Uid("MUID"),
                    reportId = MeasurementModel.ReportId("REPORT"),
                ),
                url = UrlModelFactory.build(url = "https://example.org"),
            ).webViewUrl,
        )
    }

    @Test
    fun withNone() {
        assertNull(
            MeasurementWithUrl(
                measurement = MeasurementModelFactory.build(
                    uid = null,
                    reportId = null,
                ),
                url = null,
            ).webViewUrl,
        )
    }
}
