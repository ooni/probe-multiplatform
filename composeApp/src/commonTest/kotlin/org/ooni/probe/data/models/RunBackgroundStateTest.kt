package org.ooni.probe.data.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RunBackgroundStateTest {
    @Test
    fun progress() {
        assertEquals(
            0.25,
            RunBackgroundState
                .RunningTests(
                    estimatedRuntimeOfDescriptors = listOf(1.minutes, 1.minutes),
                    descriptorIndex = 0,
                    testProgress = 0.5,
                ).progress,
        )
        assertEquals(
            0.25,
            RunBackgroundState
                .RunningTests(
                    estimatedRuntimeOfDescriptors = listOf(1.minutes, 3.minutes),
                    descriptorIndex = 0,
                    testProgress = 1.0,
                ).progress,
        )
        assertEquals(
            0.5,
            RunBackgroundState
                .RunningTests(
                    estimatedRuntimeOfDescriptors = listOf(1.seconds, 1.seconds, 1.seconds, 1.seconds),
                    descriptorIndex = 2,
                    testProgress = 0.0,
                ).progress,
        )
    }

    @Test
    fun estimatedTimeLeft() {
        assertEquals(
            90.seconds,
            RunBackgroundState
                .RunningTests(
                    estimatedRuntimeOfDescriptors = listOf(1.minutes, 1.minutes),
                    descriptorIndex = 0,
                    testProgress = 0.5,
                ).estimatedTimeLeft,
        )
        assertEquals(
            0.seconds,
            RunBackgroundState
                .RunningTests(
                    estimatedRuntimeOfDescriptors = listOf(1.minutes, 1.minutes),
                    descriptorIndex = 1,
                    testProgress = 1.0,
                ).estimatedTimeLeft,
        )
        assertEquals(
            2.minutes,
            RunBackgroundState
                .RunningTests(
                    estimatedRuntimeOfDescriptors = listOf(1.minutes, 2.minutes),
                    descriptorIndex = 1,
                    testProgress = 0.0,
                ).estimatedTimeLeft,
        )
    }
}
