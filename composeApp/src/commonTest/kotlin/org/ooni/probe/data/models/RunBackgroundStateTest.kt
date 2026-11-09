package org.ooni.probe.data.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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

    @Test
    fun noEstimates() {
        listOf(null, emptyList<Duration>()).forEach { estimates ->
            val state = RunBackgroundState.RunningTests(
                estimatedRuntimeOfDescriptors = estimates,
                descriptorIndex = 0,
                testProgress = 0.5,
            )
            assertEquals(0.0, state.progress)
            assertNull(state.estimatedTimeLeft)
        }
    }

    @Test
    fun progressWithinDescriptorAccountsForTestIndexAndTotal() {
        val state = RunBackgroundState.RunningTests(
            estimatedRuntimeOfDescriptors = listOf(1.minutes, 1.minutes),
            descriptorIndex = 0,
            testIndex = 1,
            testTotal = 4,
            testProgress = 0.5,
        )
        // (1 + 0.5) / 4 of the first half
        assertEquals(0.1875, state.progress)
        assertEquals(97.5.seconds, state.estimatedTimeLeft)
    }

    @Test
    fun lastTestOfLastDescriptorFinished() {
        val state = RunBackgroundState.RunningTests(
            estimatedRuntimeOfDescriptors = listOf(1.minutes, 1.minutes),
            descriptorIndex = 1,
            testIndex = 3,
            testTotal = 4,
            testProgress = 1.0,
        )
        assertEquals(1.0, state.progress)
        assertEquals(0.seconds, state.estimatedTimeLeft)
    }

    @Test
    fun zeroEstimatesDoNotBreakProgress() {
        val state = RunBackgroundState.RunningTests(
            estimatedRuntimeOfDescriptors = listOf(0.seconds, 0.seconds),
            descriptorIndex = 0,
            testProgress = 0.5,
        )
        assertTrue(state.progress in 0.0..1.0, "progress was ${state.progress}")
    }

    @Test
    fun subSecondEstimatesDoNotBreakProgress() {
        val state = RunBackgroundState.RunningTests(
            estimatedRuntimeOfDescriptors = listOf(500.milliseconds, 300.milliseconds),
            descriptorIndex = 0,
            testProgress = 0.5,
        )
        assertTrue(state.progress in 0.0..1.0, "progress was ${state.progress}")
    }
}
