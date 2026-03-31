package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.data.models.MeasurementWithUrl
import org.ooni.probe.data.models.UrlModel

class GetFallbackUrls(
    private val getMeasurementsWithUrl: () -> Flow<List<MeasurementWithUrl>>,
    private val getBatteryState: () -> BatteryState,
    // For testing
    private val sampleSize: Int? = null,
) {
    suspend operator fun invoke(taskOrigin: TaskOrigin): List<UrlModel> {
        val measurements = getMeasurementsWithUrl().first()
        // Remove country specific URLs
        val globalMeasurements = measurements.filter {
            val countryCode = it.url?.countryCode
            countryCode.isNullOrEmpty() ||
                countryCode.equals("XX", ignoreCase = true) ||
                countryCode.equals("ZZ", ignoreCase = true)
        }
        val sampleSize = sampleSize ?: getSampleSize(taskOrigin)

        val urls = globalMeasurements.mapNotNull { it.url }.toMutableList()
        val finalList = mutableListOf<UrlModel>()

        while (finalList.size < sampleSize && urls.isNotEmpty()) {
            val url = urls.random()
            urls.removeAll { it.url == url.url }
            finalList.add(url)
        }
        return finalList
    }

    private fun getSampleSize(taskOrigin: TaskOrigin) =
        if (taskOrigin == TaskOrigin.OoniRun) {
            1000 // The current cap in the back-end is 9999, but we're being more conservative
        } else if (getBatteryState() == BatteryState.NotCharging) {
            20
        } else {
            100
        }
}
