package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.shared.today

class DeleteOldResults(
    val getPreferenceByKey: (SettingsKey) -> Flow<Any?>,
    val deleteResultsByFilter: suspend (ResultFilter) -> Unit,
) {
    suspend operator fun invoke() {
        if (getPreferenceByKey(SettingsKey.DELETE_OLD_RESULTS).first() != true) return

        val keepThresholdInMonths =
            (getPreferenceByKey(SettingsKey.DELETE_OLD_RESULTS_THRESHOLD).first() as? Int)
                ?.coerceAtLeast(1)
                ?: DELETE_OLD_RESULTS_THRESHOLD_DEFAULT_IN_MONTHS
        val startDate = LocalDate.fromEpochDays(0)
        val endDate = LocalDate
            .today()
            .minus(keepThresholdInMonths, DateTimeUnit.MONTH)
            .coerceAtLeast(startDate)
        deleteResultsByFilter(
            ResultFilter(
                dates = ResultFilter.Date.Custom(
                    customRange = startDate..endDate,
                ),
            ),
        )
    }

    companion object {
        const val DELETE_OLD_RESULTS_THRESHOLD_DEFAULT_IN_MONTHS = 6
    }
}
