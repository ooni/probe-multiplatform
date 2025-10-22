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
    private val countNetworkCountries: () -> Flow<Long>,
) {
    operator fun invoke(): Flow<MeasurementStats> {
        val today = LocalDate.today()
        val startOfWeek = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        val startOfMonth = today.minus(today.day - 1, DateTimeUnit.DAY)
        val startOfTotal = LocalDate.fromEpochDays(0)
        return combine<Long, MeasurementStats>(
            countMeasurementsFromStartTime(today.toDateTime()),
            countMeasurementsFromStartTime(startOfWeek.toDateTime()),
            countMeasurementsFromStartTime(startOfMonth.toDateTime()),
            countMeasurementsFromStartTime(startOfTotal.toDateTime()),
            countNetworkAsns(),
            countNetworkCountries(),
        ) { values ->
            MeasurementStats(
                values[0],
                values[1],
                values[2],
                values[3],
                values[4],
                values[5],
            )
        }
    }
}
