package org.ooni.probe.shared

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

fun tickerFlow(period: Duration) =
    flow {
        while (true) {
            emit(Unit)
            delay(period)
        }
    }
