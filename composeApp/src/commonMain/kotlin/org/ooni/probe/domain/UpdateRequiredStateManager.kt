package org.ooni.probe.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds whether the passport server has reported that this app is too old to keep submitting
 * measurements (an incompatible/too-old protocol version). Kept in memory on purpose: an outdated
 * app re-fires the signal on its next submission, and an updated app never sets it, so the flag
 * self-heals after an update with no stale-state to clean up.
 */
class UpdateRequiredStateManager {
    private val updateRequired = MutableStateFlow(false)

    fun observeUpdateRequired(): StateFlow<Boolean> = updateRequired.asStateFlow()

    fun signalUpdateRequired() {
        updateRequired.value = true
    }

    fun dismiss() {
        updateRequired.value = false
    }
}
