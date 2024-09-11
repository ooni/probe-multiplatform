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
        private val descriptorIndex: Int = 0,
        val testType: TestType? = null,
        private val estimatedRuntimeOfDescriptors: List<Duration>? = null,
        private val testProgress: Double = 0.0,
        private val testIndex: Int = 0,
        private val testTotal: Int = 1,
        val log: String? = "",
    ) : TestRunState {
        val estimatedTimeLeft: Duration?
            get() {
                if (estimatedRuntimeOfDescriptors.isNullOrEmpty()) return null
                val totalTime = estimatedRuntimeOfDescriptors.sumOf { it.inWholeSeconds }.seconds
                return totalTime * (1 - progress)
            }

        val progress: Double
            get() {
                if (estimatedRuntimeOfDescriptors.isNullOrEmpty()) return 0.0

                val totalTime =
                    estimatedRuntimeOfDescriptors.sumOf { it.inWholeSeconds }.toDouble()
                val progressByDescriptor =
                    estimatedRuntimeOfDescriptors.map { it.inWholeSeconds / totalTime }

                val pastProgress = progressByDescriptor.take(descriptorIndex).sum()
                val descriptorRelativeProgress = (testIndex + testProgress) / testTotal
                val descriptorProgress =
                    progressByDescriptor[descriptorIndex] * descriptorRelativeProgress
                return pastProgress + descriptorProgress
            }
    }

    data object Stopping : TestRunState
}
