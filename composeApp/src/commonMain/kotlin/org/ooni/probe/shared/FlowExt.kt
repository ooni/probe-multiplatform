package org.ooni.probe.shared

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun tickerFlow(period: Duration) =
    flow {
        while (true) {
            emit(Unit)
            delay(period)
        }
    }

fun currentDateFlow(checkInterval: Duration = CURRENT_DATE_CHECK_INTERVAL): Flow<LocalDate> =
    tickerFlow(checkInterval)
        .map { LocalDate.today() }
        .distinctUntilChanged()

private val CURRENT_DATE_CHECK_INTERVAL = 1.minutes
