package org.ooni.probe.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.eagerOption
import kotlinx.coroutines.runBlocking
import org.ooni.probe.core.CliAutoRunGateway
import org.ooni.probe.core.CliAutoRunStatus
import org.ooni.probe.core.CliStorageConfig
import org.ooni.probe.core.DesktopCliGatewayFactory
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path

// ---- factory + injectables -----------------------------------------------------------------

fun interface CliAutoRunGatewayFactory {
    fun create(runtime: CliRuntime): CliAutoRunGateway
}

internal object ProductionCliAutoRunGatewayFactory : CliAutoRunGatewayFactory {
    override fun create(runtime: CliRuntime): CliAutoRunGateway {
        Files.createDirectories(runtime.paths.dataDir)
        // Wire through DesktopCliGatewayFactory so the CLI never constructs the storage/DataStore stack itself.
        return DesktopCliGatewayFactory.createCliAutoRunGateway(
            CliStorageConfig(
                databaseDir = runtime.paths.dataDir.toString(),
                preferencesFile = runtime.paths.preferenceDataStoreFile.toString(),
            ),
        )
    }
}

/**
 * Follows the autorun log file for appended content. Injectable so tests can supply a bounded
 * follower instead of the production blocking tail (which loops until a SIGINT/SIGTERM arrives).
 */
fun interface AutorunLogFollower {
    fun follow(
        logPath: Path,
        signals: CliSignals,
        emit: (String) -> Unit,
    )
}

internal object ProductionAutorunLogFollower : AutorunLogFollower {
    private const val POLL_INTERVAL_MS = 500L

    override fun follow(
        logPath: Path,
        signals: CliSignals,
        emit: (String) -> Unit,
    ) {
        // Start after the contents the stream command already printed, then emit only appended lines.
        var position = if (Files.exists(logPath)) Files.size(logPath) else 0L
        while (!signals.wasSignalled()) {
            val size = if (Files.exists(logPath)) Files.size(logPath) else 0L
            if (size < position) position = 0L // truncated/rotated: restart from the beginning
            if (size > position) {
                RandomAccessFile(logPath.toFile(), "r").use { file ->
                    file.seek(position)
                    var line = file.readLine()
                    while (line != null) {
                        emit(line)
                        line = file.readLine()
                    }
                    position = file.filePointer
                }
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
    }
}

/** The single deterministic CLI autorun log file, under `<ooniHome>/logs`. */
internal fun autorunLogPath(runtime: CliRuntime): Path = runtime.paths.logsDir.resolve("autorun.log")

// ---- autorun (parent) ----------------------------------------------------------------------

internal class AutorunCommand(
    output: CliOutput,
    autorunGatewayFactory: CliAutoRunGatewayFactory,
    autorunLogFollower: AutorunLogFollower,
    signals: CliSignals,
) : CliktCommand(name = "autorun") {
    init {
        context { helpOptionNames = emptySet() }
        eagerOption("-h", "--help") {
            output.stdout(usage())
            throw ProgramResult(0)
        }
        subcommands(
            AutorunStartCommand(output),
            AutorunStopCommand(output),
            AutorunStatusCommand(output, autorunGatewayFactory),
            AutorunLogCommand(output, autorunLogFollower, signals),
        )
    }

    override fun run() = Unit

    companion object {
        fun usage() = """
            Usage: ooniprobe autorun <command>

            Commands:
              status  Print autorun readiness (enabled, constraints, scheduled tests)
              log     Show or stream the autorun log (show | stream)
              start   Start autorun service supervision (unsupported on this platform)
              stop    Stop autorun service supervision (unsupported on this platform)
        """.trimIndent()
    }
}

// ---- autorun start / stop (unsupported service supervision) --------------------------------

// Real OS service supervision (launchd/systemd/Windows service) is out of scope for T12: these
// return a deterministic unsupported error (nonzero exit) instead of faking success or installing a
// duplicate scheduler. Recorded as a parity blocker in
// .omo/artifacts/cli-completion-full-parity/parity-blockers.md (T12 — autorun start/stop supervision).
internal const val AUTORUN_SUPERVISION_UNSUPPORTED =
    "autorun service supervision is not implemented on this platform."

internal class AutorunStartCommand(
    private val output: CliOutput,
) : CliktCommand(name = "start") {
    override fun run() = output.fail("$AUTORUN_SUPERVISION_UNSUPPORTED Cannot start.", RUNTIME_ERROR_EXIT_CODE)
}

internal class AutorunStopCommand(
    private val output: CliOutput,
) : CliktCommand(name = "stop") {
    override fun run() = output.fail("$AUTORUN_SUPERVISION_UNSUPPORTED Cannot stop.", RUNTIME_ERROR_EXIT_CODE)
}

// ---- autorun status ------------------------------------------------------------------------

internal class AutorunStatusCommand(
    private val output: CliOutput,
    private val gatewayFactory: CliAutoRunGatewayFactory,
) : CliktCommand(name = "status") {
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()

    override fun run() {
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        val gateway = gatewayFactory.create(runtime)
        try {
            val status = runBlocking { gateway.status() }
            emit(runtime, status)
        } finally {
            gateway.close()
        }
    }

    private fun emit(runtime: CliRuntime, status: CliAutoRunStatus) {
        if (runtime.jsonOutput) {
            output.stdout(
                CliJson.obj(
                    "enabled" to CliJson.bool(status.enabled),
                    "wifi_only" to CliJson.bool(status.wifiOnly),
                    "only_while_charging" to CliJson.bool(status.onlyWhileCharging),
                    "constraints_satisfied" to CliJson.bool(status.constraintsSatisfied),
                    "descriptors" to CliJson.num(status.descriptorCount.toLong()),
                    "tests" to CliJson.num(status.testCount.toLong()),
                ),
            )
        } else {
            output.stdout("Autorun: ${if (status.enabled) "enabled" else "disabled"}")
            output.stdout("Wi-Fi only: ${yesNo(status.wifiOnly)}")
            output.stdout("Only while charging: ${yesNo(status.onlyWhileCharging)}")
            output.stdout("Constraints satisfied: ${yesNo(status.constraintsSatisfied)}")
            output.stdout("Scheduled: ${status.descriptorCount} group(s), ${status.testCount} test(s)")
        }
    }
}

// ---- autorun log show / stream -------------------------------------------------------------

internal class AutorunLogCommand(
    output: CliOutput,
    follower: AutorunLogFollower,
    signals: CliSignals,
) : CliktCommand(name = "log") {
    init {
        context { helpOptionNames = emptySet() }
        eagerOption("-h", "--help") {
            output.stdout(usage())
            throw ProgramResult(0)
        }
        subcommands(
            AutorunLogShowCommand(output),
            AutorunLogStreamCommand(output, follower, signals),
        )
    }

    override fun run() = Unit

    companion object {
        fun usage() = """
            Usage: ooniprobe autorun log <command>

            Commands:
              show    Print the current autorun log file contents
              stream  Print then follow the autorun log file
        """.trimIndent()
    }
}

internal class AutorunLogShowCommand(
    private val output: CliOutput,
) : CliktCommand(name = "show") {
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()

    override fun run() {
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        val logPath = autorunLogPath(runtime)
        if (!Files.exists(logPath)) {
            output.stdout("No autorun log file at $logPath")
            return
        }
        Files.readAllLines(logPath).forEach(output.stdout)
    }
}

internal class AutorunLogStreamCommand(
    private val output: CliOutput,
    private val follower: AutorunLogFollower,
    private val signals: CliSignals,
) : CliktCommand(name = "stream") {
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()

    override fun run() {
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        val logPath = autorunLogPath(runtime)
        // Print whatever is already on disk (deterministic), then follow only appended content.
        if (Files.exists(logPath)) {
            Files.readAllLines(logPath).forEach(output.stdout)
        } else {
            output.stdout("No autorun log file at $logPath")
        }
        follower.follow(logPath, signals, output.stdout)
        if (signals.wasSignalled()) throw ProgramResult(SIGINT_EXIT_CODE)
    }
}

// ---- shared helpers ------------------------------------------------------------------------

private fun CliOutput.fail(message: String, code: Int): Nothing {
    stderr(message)
    throw ProgramResult(code)
}

private fun yesNo(value: Boolean): String = if (value) "yes" else "no"
