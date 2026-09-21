package org.ooni.probe.domain

/** Handle returned when registering a run-cancellation listener; call [dismiss] to remove it. */
fun interface CancelListenerCallback {
    fun dismiss()
}
