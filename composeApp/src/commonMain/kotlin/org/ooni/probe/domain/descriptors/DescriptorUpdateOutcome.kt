package org.ooni.probe.domain.descriptors

/**
 * What a descriptor update run actually did, so the Android worker can decide whether retrying is
 * worth anything. Kept in common code (and free of WorkManager types) so the policy is unit
 * testable without a device.
 */
data class DescriptorUpdateOutcome(
    val attempted: Int = 0,
    val successes: Int = 0,
    val networkFailures: Int = 0,
    val otherFailures: Int = 0,
) {
    /**
     * Retry only when every single attempt failed for lack of a network, and only a bounded number
     * of times - after that the periodic worker will pick it up on its normal schedule.
     *
     * Deliberately excluded:
     * - `attempted == 0` (nothing installed to update): there is nothing to retry.
     * - partial success: the descriptors that failed are a minority; re-running the whole batch
     *   would re-fetch the ones that already worked.
     * - parse, config or HTTP contract failures: retrying re-sends a request the server already
     *   answered. Those are bugs or server-side problems, and a retry loop would only hide them.
     */
    fun shouldRetry(runAttemptCount: Int): Boolean =
        attempted > 0 &&
            networkFailures == attempted &&
            runAttemptCount < MAX_NETWORK_RETRY_ATTEMPTS

    companion object {
        const val MAX_NETWORK_RETRY_ATTEMPTS = 3
    }
}
