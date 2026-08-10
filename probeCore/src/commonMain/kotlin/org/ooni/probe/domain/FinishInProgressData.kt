package org.ooni.probe.domain

class FinishInProgressData(
    private val markResultsAsDone: suspend () -> Unit,
) {
    suspend operator fun invoke() {
        markResultsAsDone()
    }
}
