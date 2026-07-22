package org.ooni.probe.domain.descriptors

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DescriptorUpdateOutcomeTest {
    @Test
    fun nothingAttemptedDoesNotRetry() {
        val outcome = DescriptorUpdateOutcome(attempted = 0)

        assertFalse(outcome.shouldRetry(runAttemptCount = 0))
    }

    @Test
    fun allNetworkFailuresRetryWithinTheBound() {
        val outcome = DescriptorUpdateOutcome(attempted = 3, networkFailures = 3)

        assertTrue(outcome.shouldRetry(runAttemptCount = 0))
        assertTrue(outcome.shouldRetry(runAttemptCount = 1))
        assertTrue(outcome.shouldRetry(runAttemptCount = 2))
    }

    @Test
    fun allNetworkFailuresStopRetryingAfterThreeAttempts() {
        val outcome = DescriptorUpdateOutcome(attempted = 3, networkFailures = 3)

        // Give up and let the periodic worker pick it up on its normal schedule.
        assertFalse(outcome.shouldRetry(runAttemptCount = 3))
        assertFalse(outcome.shouldRetry(runAttemptCount = 4))
    }

    @Test
    fun partialSuccessDoesNotRetry() {
        val outcome = DescriptorUpdateOutcome(
            attempted = 3,
            successes = 2,
            networkFailures = 1,
        )

        assertFalse(outcome.shouldRetry(runAttemptCount = 0))
    }

    @Test
    fun parseAndContractFailuresNeverRetry() {
        val outcome = DescriptorUpdateOutcome(attempted = 2, otherFailures = 2)

        assertFalse(
            outcome.shouldRetry(runAttemptCount = 0),
            "an HTTP 4xx/5xx or parse failure must not be retried as if it were transient",
        )
    }

    @Test
    fun mixedNetworkAndOtherFailuresDoNotRetry() {
        val outcome = DescriptorUpdateOutcome(
            attempted = 2,
            networkFailures = 1,
            otherFailures = 1,
        )

        assertFalse(outcome.shouldRetry(runAttemptCount = 0))
    }

    @Test
    fun fullSuccessDoesNotRetry() {
        val outcome = DescriptorUpdateOutcome(attempted = 2, successes = 2)

        assertFalse(outcome.shouldRetry(runAttemptCount = 0))
    }
}
