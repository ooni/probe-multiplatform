package org.ooni.probe.data.models

import kotlinx.datetime.LocalDateTime
import org.ooni.engine.models.TestType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface TestRunState {
    data class Idle(
        val lastTestAt: LocalDateTime? = null,
        val justFinishedTest: Boolean = false,
    ) : TestRunState

    data class Running(
        val descriptorName: String? = null,
        val testType: TestType? = null,
        val estimatedRuntime: List<Duration>? = null,
        val testProgress: Double = 0.0,
        val testIndex: Int = 0,
        val log: String? = "",
    ) : TestRunState {
        val estimatedTimeLeft: Duration?
            get() {
                if (estimatedRuntime == null) return null
                val remainingTests =
                    estimatedRuntime.drop(testIndex + 1)
                        .sumOf { it.inWholeSeconds }.seconds
                val remainingFromCurrentTest =
                    (estimatedRuntime[testIndex] * (1 - testProgress)).coerceAtLeast(0.seconds)
                return remainingTests + remainingFromCurrentTest
            }
    }

    data object Stopping : TestRunState
}
