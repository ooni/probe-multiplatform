package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import org.ooni.probe.data.models.MeasurementStats
import org.ooni.probe.shared.toLocalDateTime
import org.ooni.probe.shared.today

class GetMeasurementStats(
    private val countMeasurementsFromStartTime: (LocalDateTime) -> Flow<Long>,
    private val countTotalNetworks: () -> Flow<Long>,
    private val countTotalNetworkCountries: () -> Flow<Long>,
) {
    operator fun invoke(): Flow<MeasurementStats> {
        val today = LocalDate.today()
        val startOfWeek = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        val startOfMonth = today.minus(today.day - 1, DateTimeUnit.DAY)
        LocalDate.today()
        return combine(
            countMeasurementsFromStartTime(today.toDateTime()),
            countMeasurementsFromStartTime(startOfWeek.toDateTime()),
            countMeasurementsFromStartTime(startOfMonth.toDateTime()),
            countTotalNetworks(),
            countTotalNetworkCountries(),
            ::MeasurementStats,
        )
    }
}

private fun LocalDate.toDateTime() = atStartOfDayIn(TimeZone.currentSystemDefault()).toLocalDateTime()
