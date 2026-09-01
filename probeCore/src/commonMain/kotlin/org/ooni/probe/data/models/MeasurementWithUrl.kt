package org.ooni.probe.data.models

data class MeasurementWithUrl(
    val measurement: MeasurementModel,
    val url: UrlModel?,
)
