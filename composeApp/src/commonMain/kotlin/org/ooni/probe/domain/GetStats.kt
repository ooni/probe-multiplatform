package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import org.ooni.probe.data.models.MeasurementStats
import org.ooni.probe.shared.toDateTime
import org.ooni.probe.shared.today

class GetStats(
    private val countMeasurementsFromStartTime: (LocalDateTime) -> Flow<Long>,
    private val countNetworkAsns: () -> Flow<Long>,
    private val getNetworkCountries: () -> Flow<List<String>>,
    private val getCountryNameByCode: (String) -> String,
) {
    operator fun invoke(): Flow<MeasurementStats> {
        val today = LocalDate.today()
        val startOfWeek = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        val startOfMonth = today.minus(today.day - 1, DateTimeUnit.DAY)
        val startOfTotal = LocalDate.fromEpochDays(0)
        return combine(
            combine(
                countMeasurementsFromStartTime(today.toDateTime()),
                countMeasurementsFromStartTime(startOfWeek.toDateTime()),
                countMeasurementsFromStartTime(startOfMonth.toDateTime()),
                countMeasurementsFromStartTime(startOfTotal.toDateTime()),
                countNetworkAsns(),
            ) { it },
            getNetworkCountries(),
        ) { values, countries ->
            MeasurementStats(
                measurementsToday = values[0],
                measurementsWeek = values[1],
                measurementsMonth = values[2],
                measurementsTotal = values[3],
                networks = values[4],
                countries = countries
                    .map { getCountryNameByCode(it) }
                    .sorted(),
            )
        }
    }
}
