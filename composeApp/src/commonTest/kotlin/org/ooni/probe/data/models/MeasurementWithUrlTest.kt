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
        val itemWithoutUrl = MeasurementWithUrl(
            measurement = MeasurementModelFactory.build(
                uid = null,
                reportId = MeasurementModel.ReportId("REPORT"),
            ),
            url = null,
        )
        assertEquals(
            "${OrganizationConfig.explorerUrl}/measurement/REPORT",
            itemWithoutUrl.explorerUrl,
        )
        assertEquals(
            "${OrganizationConfig.explorerUrl}/measurement/REPORT?webview=true&language=${Locale.current.languageRegionString}",
            itemWithoutUrl.webViewUrl,
        )

        val itemWithUrl = MeasurementWithUrl(
            measurement = MeasurementModelFactory.build(
                uid = null,
                reportId = MeasurementModel.ReportId("REPORT"),
            ),
            url = UrlModelFactory.build(url = "https://example.org"),
        )
        assertEquals(
            "${OrganizationConfig.explorerUrl}/measurement/REPORT?input=https%3A%2F%2Fexample.org",
            itemWithUrl.explorerUrl,
        )
        assertEquals(
            "${OrganizationConfig.explorerUrl}/measurement/REPORT?input=https%3A%2F%2Fexample.org&webview=true&language=${Locale.current.languageRegionString}",
            itemWithUrl.webViewUrl,
        )
    }

    @Test
    fun withUid() {
        val itemWithoutUrl = MeasurementWithUrl(
            measurement = MeasurementModelFactory.build(
                uid = MeasurementModel.Uid("MUID"),
                reportId = MeasurementModel.ReportId("REPORT"),
            ),
            url = null,
        )
        assertEquals(
            "${OrganizationConfig.explorerUrl}/m/MUID",
            itemWithoutUrl.explorerUrl,
        )
        assertEquals(
            "${OrganizationConfig.explorerUrl}/m/MUID?webview=true&language=${Locale.current.languageRegionString}",
            itemWithoutUrl.webViewUrl,
        )

        val itemWithUrl = MeasurementWithUrl(
            measurement = MeasurementModelFactory.build(
                uid = MeasurementModel.Uid("MUID"),
                reportId = MeasurementModel.ReportId("REPORT"),
            ),
            url = UrlModelFactory.build(url = "https://example.org"),
        )
        assertEquals(
            "${OrganizationConfig.explorerUrl}/m/MUID",
            itemWithUrl.explorerUrl,
        )
        assertEquals(
            "${OrganizationConfig.explorerUrl}/m/MUID?webview=true&language=${Locale.current.languageRegionString}",
            itemWithUrl.webViewUrl,
        )
    }

    @Test
    fun withNone() {
        val item = MeasurementWithUrl(
            measurement = MeasurementModelFactory.build(
                uid = null,
                reportId = null,
            ),
            url = null,
        )
        assertNull(item.explorerUrl)
        assertNull(item.webViewUrl)
    }
}
