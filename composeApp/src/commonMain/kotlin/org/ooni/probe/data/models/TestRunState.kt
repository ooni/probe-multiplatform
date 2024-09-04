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
        val descriptor: Descriptor? = null,
        val descriptorIndex: Int = 0,
        val testType: TestType? = null,
        val estimatedRuntimeOfDescriptors: List<Duration>? = null,
        val testProgress: Double = 0.0,
        val testIndex: Int = 0,
        val testTotal: Int = 1,
        val log: String? = "",
    ) : TestRunState {
        val estimatedTimeLeft: Duration?
            get() {
                if (estimatedRuntimeOfDescriptors == null) return null
                val remainingDescriptorsEstimatedTime =
                    estimatedRuntimeOfDescriptors.drop(descriptorIndex + 1)
                        .sumOf { it.inWholeSeconds }.seconds
                val currentDescriptorEstimatedTime = estimatedRuntimeOfDescriptors[descriptorIndex]
                val descriptorProgress = (testIndex + testProgress) / testTotal
                val currentDescriptorRemainingBasedOnTest =
                    (currentDescriptorEstimatedTime * (1 - descriptorProgress))
                        .coerceAtLeast(0.seconds)
                return remainingDescriptorsEstimatedTime + currentDescriptorRemainingBasedOnTest
            }
    }

    data object Stopping : TestRunState
}
