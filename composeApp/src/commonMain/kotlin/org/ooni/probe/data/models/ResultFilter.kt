package org.ooni.probe.data.models

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
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
        val range: ClosedRange<LocalDate>,
    ) {
        data object AnyDate :
            Date(LocalDate(2000, 1, 1)..LocalDate.today())

        data object Today :
            Date(LocalDate.today()..LocalDate.today())

        data object FromSevenDaysAgo :
            Date(LocalDate.today().minus(DatePeriod(days = 7))..LocalDate.today())

        data object FromOneMonthAgo :
            Date(LocalDate.today().minus(DatePeriod(months = 1))..LocalDate.today())

        data class Custom(val customRange: ClosedRange<LocalDate>) : Date(customRange)
    }

    companion object {
        const val LIMIT = 100L
    }
}
