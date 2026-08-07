package org.ooni.probe.cli

import org.ooni.probe.core.CliAutoRunStatus
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutorunCommandFailureTest {
    private val runtime = CliRuntime(
        paths = CliPathLayout.create(ooniHome = Path.of("/tmp/ooni-cli-autorun-fail-home"), tempDir = Path.of("/tmp")),
    )

    @Test
    fun startIsUnsupportedWithDeterministicNonzeroExitAndNoGatewayUse() {
        val factory = FakeCliAutoRunGatewayFactory(disabledStatus())
        val result = runCli("autorun", "start", autoRun = factory)

        assertEquals(RUNTIME_ERROR_EXIT_CODE, result.code)
        assertTrue(result.stdout.isEmpty())
        assertTrue(result.stderr.any { it.contains(AUTORUN_SUPERVISION_UNSUPPORTED) }, result.stderr.toString())
        // No autorun gateway is constructed and no scheduler/foreground run is started.
        assertEquals(0, factory.creations)
    }

    @Test
    fun stopIsUnsupportedWithDeterministicNonzeroExitAndNoGatewayUse() {
        val factory = FakeCliAutoRunGatewayFactory(disabledStatus())
        val result = runCli("autorun", "stop", autoRun = factory)

        assertEquals(RUNTIME_ERROR_EXIT_CODE, result.code)
        assertTrue(result.stdout.isEmpty())
        assertTrue(result.stderr.any { it.contains(AUTORUN_SUPERVISION_UNSUPPORTED) }, result.stderr.toString())
        assertEquals(0, factory.creations)
    }

    @Test
    fun unknownAutorunSubcommandFailsWithUsageExit() {
        val factory = FakeCliAutoRunGatewayFactory(disabledStatus())
        val result = runCli("autorun", "bogus", autoRun = factory)

        assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
        assertTrue(result.stderr.isNotEmpty())
        assertEquals(0, factory.creations)
    }

    @Test
    fun statusRejectsExtraArgumentsWithUsageExitAndNoGatewayUse() {
        val factory = FakeCliAutoRunGatewayFactory(disabledStatus())
        val result = runCli("autorun", "status", "extra", autoRun = factory)

        assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
        assertTrue(result.stderr.isNotEmpty())
        // Parse fails before the command body runs, so the gateway is never constructed.
        assertEquals(0, factory.creations)
    }

    @Test
    fun logShowRejectsExtraArgumentsWithUsageExit() {
        val result = runCli("autorun", "log", "show", "extra")

        assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
        assertTrue(result.stderr.isNotEmpty())
    }

    @Test
    fun constraintDeniedStatusIsReportedDeterministically() {
        val factory = FakeCliAutoRunGatewayFactory(
            CliAutoRunStatus(
                enabled = true,
                wifiOnly = false,
                onlyWhileCharging = false,
                constraintsSatisfied = false,
                descriptorCount = 1,
                testCount = 2,
            ),
        )
        val result = runCli("autorun", "status", autoRun = factory)

        // Constraint denial is a reportable state, not an error: exit 0, deterministic output.
        assertEquals(0, result.code)
        assertTrue(result.stdout.contains("Autorun: enabled"))
        assertTrue(result.stdout.contains("Constraints satisfied: no"))
    }

    private fun runCli(
        vararg args: String,
        autoRun: FakeCliAutoRunGatewayFactory = FakeCliAutoRunGatewayFactory(disabledStatus()),
    ): AutorunFailureResult {
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()
        val code = OoniprobeCli(
            runtime = runtime,
            autoRunGatewayFactory = autoRun,
            autoRunLogFollower = RecordingAutorunLogFollower(),
            input = { null },
        ).run(args.toList().toTypedArray(), stdout::add, stderr::add)
        return AutorunFailureResult(code, stdout, stderr)
    }

    private fun disabledStatus() =
        CliAutoRunStatus(
            enabled = false,
            wifiOnly = false,
            onlyWhileCharging = false,
            constraintsSatisfied = false,
            descriptorCount = 0,
            testCount = 0,
        )

    private data class AutorunFailureResult(
        val code: Int,
        val stdout: List<String>,
        val stderr: List<String>,
    )
}
