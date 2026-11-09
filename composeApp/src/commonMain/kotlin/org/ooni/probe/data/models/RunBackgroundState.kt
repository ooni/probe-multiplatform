package org.ooni.probe.data.models

import org.ooni.engine.models.TestType
import org.ooni.probe.domain.UploadMissingMeasurements
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface RunBackgroundState {
    data object Idle : RunBackgroundState

    data object Preparing : RunBackgroundState

    data class UploadingMissingResults(
        val state: UploadMissingMeasurements.State,
    ) : RunBackgroundState

    data class RunningTests(
        val descriptor: DescriptorItem? = null,
        private val descriptorIndex: Int = 0,
        val testType: TestType? = null,
        private val estimatedRuntimeOfDescriptors: List<Duration>? = null,
        private val testProgress: Double = 0.0,
        private val testIndex: Int = 0,
        private val testTotal: Int = 1,
        val log: String? = "",
    ) : RunBackgroundState {
        val estimatedTimeLeft: Duration? by lazy {
            if (estimatedRuntimeOfDescriptors.isNullOrEmpty()) return@lazy null
            val totalTime = estimatedRuntimeOfDescriptors.sumOf { it.inWholeSeconds }.seconds
            totalTime * (1 - progress)
        }

        val progress by lazy {
            if (estimatedRuntimeOfDescriptors.isNullOrEmpty()) return@lazy 0.0

            val totalTime = estimatedRuntimeOfDescriptors
                .sumOf { it.inWholeSeconds }
                .toDouble()
                .coerceAtLeast(1.0)
            val progressByDescriptor =
                estimatedRuntimeOfDescriptors.map { it.inWholeSeconds / totalTime }
            val pastProgress = progressByDescriptor.take(descriptorIndex).sum()
            val descriptorRelativeProgress = (testIndex + testProgress) / testTotal
            val descriptorProgress =
                progressByDescriptor[descriptorIndex] * descriptorRelativeProgress
            pastProgress + descriptorProgress
        }
    }

    data object Stopping : RunBackgroundState
}
