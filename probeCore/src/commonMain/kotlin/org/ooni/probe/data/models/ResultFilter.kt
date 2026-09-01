package org.ooni.probe.data.models

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.shared.today

data class ResultFilter(
    val descriptors: List<Descriptor> = emptyList(),
    val networks: List<NetworkModel> = emptyList(),
    val taskOrigin: TaskOrigin? = null,
    val dates: Date = Date.AnyDate,
    val limit: Long = LIMIT,
) {
    val isAll get() = this == ResultFilter()

    val filterCount
        get() = descriptors.size +
            networks.size +
            (if (taskOrigin == ResultFilter().taskOrigin) 0 else 1) +
            (if (dates == ResultFilter().dates) 0 else 1)

    sealed class Date(
        val range: () -> ClosedRange<LocalDate>,
    ) {
        data object AnyDate :
            Date(range = { MIN_DATE..MAX_DATE })

        data object Today :
            Date(range = { LocalDate.today()..MAX_DATE })

        data object FromSevenDaysAgo :
            Date(range = { LocalDate.today().minus(DatePeriod(days = 7))..MAX_DATE })

        data object FromOneMonthAgo :
            Date(range = { LocalDate.today().minus(DatePeriod(months = 1))..MAX_DATE })

        data class Custom(
            val customRange: ClosedRange<LocalDate>,
        ) : Date({ customRange })
    }

    companion object {
        const val LIMIT = 100L
        private val MIN_DATE = LocalDate(2000, 1, 1)
        private val MAX_DATE = LocalDate.today().plus(1, DateTimeUnit.YEAR)
    }
}
