package org.ooni.probe.cli

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.ooni.probe.core.CliRunGateway
import org.ooni.probe.core.CliRunOptions
import org.ooni.probe.core.CliRunProgress
import org.ooni.probe.core.CliUploadGateway
import org.ooni.probe.core.CliUploadProgress
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.probe.data.models.RunSpecification
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** In-process signal-dispatcher tests: no real OS signals are ever sent. */
class CliSignalsTest {
    @Test
    fun intSignalInvokesActiveCancelAndMarksSignalled() {
        val register = FakeSignalRegister()
        val signals = CliSignals(register = register::register).apply { install() }
        var cancelled = false
        signals.setActive { cancelled = true }

        register.dispatch("INT")

        assertTrue(cancelled, "active cancel must be invoked on SIGINT")
        assertTrue(signals.wasSignalled())
    }

    @Test
    fun termSignalInvokesActiveCancelAndMarksSignalled() {
        val register = FakeSignalRegister()
        val signals = CliSignals(register = register::register).apply { install() }
        var cancelled = false
        signals.setActive { cancelled = true }

        register.dispatch("TERM")

        assertTrue(cancelled, "active cancel must be invoked on SIGTERM")
        assertTrue(signals.wasSignalled())
    }

    @Test
    fun signalWithNoActiveCancellableDoesNotThrow() {
        val register = FakeSignalRegister()
        val signals = CliSignals(register = register::register).apply { install() }

        register.dispatch("INT")

        assertTrue(signals.wasSignalled())
    }

    @Test
    fun clearActivePreventsCancelInvocation() {
        val register = FakeSignalRegister()
        val signals = CliSignals(register = register::register).apply { install() }
        var cancelled = false
        signals.setActive { cancelled = true }
        signals.clearActive()

        register.dispatch("TERM")

        assertFalse(cancelled, "cleared cancellable must not be invoked")
        assertTrue(signals.wasSignalled())
    }
}

/** Drives a real run/upload command with a suspending fake gateway and a simulated signal. */
class CliSignalsCancellationTest {
    @Test
    fun sigintCancelsInFlightRunAndReturns130() {
        val register = FakeSignalRegister()
        val signals = CliSignals(register = register::register).apply { install() }
        val gateway = SuspendingRunGateway()

        val execution = launchCli(signals, arrayOf("--batch", "run", "performance"), runGateway = gateway)
        awaitStarted(gateway.started)
        register.dispatch("INT")
        execution.join()

        assertTrue(gateway.cancelCalled, "run gateway cancel() must be invoked")
        assertTrue(gateway.closed, "run gateway close() must run for cleanup")
        assertEquals(SIGINT_EXIT_CODE, execution.code)
    }

    @Test
    fun sigtermCancelsInFlightUploadAndReturns130() {
        val register = FakeSignalRegister()
        val signals = CliSignals(register = register::register).apply { install() }
        val gateway = SuspendingUploadGateway()

        val execution = launchCli(signals, arrayOf("--batch", "upload", "all"), uploadGateway = gateway)
        awaitStarted(gateway.started)
        register.dispatch("TERM")
        execution.join()

        assertTrue(gateway.cancelCalled, "upload gateway cancel() must be invoked")
        assertTrue(gateway.closed, "upload gateway close() must run for cleanup")
        assertEquals(SIGINT_EXIT_CODE, execution.code)
    }
}

/** Cleanup-failure after cancel: deterministic nonzero result, no prompt/hang in batch mode. */
class CliSignalsFailureTest {
    @Test
    fun cleanupFailureAfterCancelYieldsDeterministicNonZeroWithoutHang() {
        val register = FakeSignalRegister()
        val signals = CliSignals(register = register::register).apply { install() }
        val gateway = SuspendingRunGateway(closeThrows = true)

        val execution = launchCli(signals, arrayOf("--batch", "run", "performance"), runGateway = gateway)
        awaitStarted(gateway.started)
        register.dispatch("INT")
        execution.join()

        assertTrue(gateway.cancelCalled, "run gateway cancel() must still be invoked")
        assertFalse(execution.thread.isAlive, "command must not hang after cleanup failure")
        assertTrue(execution.code != 0, "cleanup failure must yield a nonzero exit code, got ${execution.code}")
    }
}

// ---- shared harness -------------------------------------------------------------------------

private const val JOIN_TIMEOUT_MS = 10_000L

/** Captures signal handlers instead of installing real OS handlers, so tests dispatch by hand. */
internal class FakeSignalRegister {
    private val handlers = mutableMapOf<String, () -> Unit>()

    fun register(name: String, handler: () -> Unit) {
        handlers[name] = handler
    }

    fun dispatch(name: String) {
        (handlers[name] ?: error("no handler registered for $name"))()
    }
}

private class CliExecution {
    @Volatile
    var code: Int = Int.MIN_VALUE
    lateinit var thread: Thread

    fun join() {
        thread.join(JOIN_TIMEOUT_MS)
        assertFalse(thread.isAlive, "CLI thread did not finish within ${JOIN_TIMEOUT_MS}ms")
    }
}

private fun awaitStarted(started: CompletableDeferred<Unit>) {
    runBlocking { withTimeout(JOIN_TIMEOUT_MS) { started.await() } }
}

/**
 * Runs the CLI on a background thread (the command blocks on runBlocking) so the test thread can
 * dispatch a simulated signal while the run/upload is in flight, then reads the exit code after join.
 */
private fun launchCli(
    signals: CliSignals,
    args: Array<String>,
    runGateway: CliRunGateway? = null,
    uploadGateway: CliUploadGateway? = null,
): CliExecution {
    val runtime = CliRuntime(
        paths = CliPathLayout.create(ooniHome = Path.of("/tmp/ooni-cli-signals-home"), tempDir = Path.of("/tmp")),
    )
    val execution = CliExecution()
    execution.thread = Thread {
        execution.code = OoniprobeCli(
            runtime = runtime,
            storageGatewayFactory = { FakeRunStorageGateway(onboarded = true) },
            uploadGatewayFactory = {
                uploadGateway ?: error("upload gateway not expected")
            },
            runGatewayFactory = {
                runGateway ?: error("run gateway not expected")
            },
            input = { null },
            signals = signals,
        ).run(args, {}, {})
    }
    execution.thread.start()
    return execution
}

// ---- suspending fakes -----------------------------------------------------------------------

private class SuspendingRunGateway(
    private val closeThrows: Boolean = false,
) : CliRunGateway {
    val started = CompletableDeferred<Unit>()
    private val cancelled = CompletableDeferred<Unit>()

    @Volatile
    var cancelCalled = false

    @Volatile
    var closed = false

    override fun run(spec: RunSpecification, options: CliRunOptions): Flow<CliRunProgress> =
        flow {
            emit(CliRunProgress(CliRunProgress.Phase.RunningTests, testType = "ndt"))
            started.complete(Unit)
            cancelled.await() // suspend until cancel() completes the run gracefully
        }

    override suspend fun descriptors(): List<Descriptor> = FakeCliRunGateway().descriptors()

    override fun cancel() {
        cancelCalled = true
        cancelled.complete(Unit)
    }

    override fun close() {
        closed = true
        if (closeThrows) throw IllegalStateException("simulated cleanup failure")
    }
}

private class SuspendingUploadGateway : CliUploadGateway {
    val started = CompletableDeferred<Unit>()
    private val cancelled = CompletableDeferred<Unit>()

    @Volatile
    var cancelCalled = false

    @Volatile
    var closed = false

    override fun uploadMissing(filter: MeasurementsFilter): Flow<CliUploadProgress> =
        flow {
            emit(CliUploadProgress(0, 0, 1, finished = false))
            started.complete(Unit)
            cancelled.await() // suspend until cancel() completes the upload gracefully
        }

    override fun cancel() {
        cancelCalled = true
        cancelled.complete(Unit)
    }

    override fun close() {
        closed = true
    }
}
