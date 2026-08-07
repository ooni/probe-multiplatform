package org.ooni.probe.cli

/** Conventional POSIX exit code for a process terminated by SIGINT (128 + 2). */
internal const val SIGINT_EXIT_CODE = 130

/**
 * CLI-only SIGINT/SIGTERM handling. On a signal it marks itself signalled and invokes the currently
 * active cancellable (a run/upload gateway's `cancel()`), so the in-flight flow completes gracefully
 * through the canonical core cancellation path rather than a process-kill that bypasses cleanup.
 *
 * Registration is injectable so tests can dispatch simulated signals by invoking the captured
 * handler directly, without sending real OS signals. The default [register] wires the real
 * `sun.misc.Signal` handlers.
 *
 * A shutdown hook is intentionally NOT used: it cannot set a custom exit code and runs too late for
 * a graceful in-run cancel.
 */
class CliSignals(
    private val register: (name: String, handler: () -> Unit) -> Unit = ::registerRealSignal,
) {
    @Volatile
    private var signalled = false

    @Volatile
    private var active: (() -> Unit)? = null

    /** Registers the SIGINT + SIGTERM handlers. Call once during process startup. */
    fun install() {
        register("INT") { onSignal() }
        register("TERM") { onSignal() }
    }

    /** Marks [cancel] as the cancellable to invoke when a signal arrives. */
    fun setActive(cancel: () -> Unit) {
        active = cancel
    }

    fun clearActive() {
        active = null
    }

    fun wasSignalled(): Boolean = signalled

    private fun onSignal() {
        signalled = true
        active?.invoke()
    }

    private companion object {
        fun registerRealSignal(name: String, handler: () -> Unit) {
            sun.misc.Signal.handle(sun.misc.Signal(name)) { handler() }
        }
    }
}
