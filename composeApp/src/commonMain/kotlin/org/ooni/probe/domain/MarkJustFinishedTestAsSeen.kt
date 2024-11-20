package org.ooni.probe.domain

import org.ooni.probe.data.models.RunBackgroundState

class MarkJustFinishedTestAsSeen(
    private val setRunBackgroundState: ((RunBackgroundState) -> RunBackgroundState) -> Unit,
) {
    operator fun invoke() {
        setRunBackgroundState { state ->
            if (state is RunBackgroundState.Idle && state.justFinishedTest) {
                state.copy(justFinishedTest = false)
            } else {
                state
            }
        }
    }
}
