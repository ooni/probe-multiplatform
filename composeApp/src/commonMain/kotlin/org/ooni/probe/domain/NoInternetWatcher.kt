package org.ooni.probe.domain

import org.ooni.engine.models.NetworkType

/**
 * Counts consecutive checks without Internet across a whole run (all descriptors and tests).
 * Checks happen after each finished measurement, or once per test that produced no measurement.
 * Create one instance per run, so no state carries over between runs.
 */
class NoInternetWatcher(
    private val networkTypeFinder: () -> NetworkType,
) {
    private var noInternetCounter = 0

    fun checkIfShouldCancelDueToNoInternet(): Boolean {
        if (networkTypeFinder() == NetworkType.NoInternet) {
            noInternetCounter += 1
        } else {
            noInternetCounter = 0
        }
        return noInternetCounter >= NO_INTERNET_CANCEL_THRESHOLD
    }

    companion object {
        // Cancel run after this amount of consecutive checks without Internet
        const val NO_INTERNET_CANCEL_THRESHOLD = 3
    }
}
