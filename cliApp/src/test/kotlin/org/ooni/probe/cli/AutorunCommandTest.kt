package org.ooni.probe.cli

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.ooni.probe.core.CliAutoRunGateway
import org.ooni.probe.core.CliAutoRunStatus
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutorunCommandTest {
    private val runtime = CliRuntime(
        paths = CliPathLayout.create(ooniHome = Path.of("/tmp/ooni-cli-autorun-home"), tempDir = Path.of("/tmp")),
    )

    // ---- status ----

    @Test
    fun statusHumanPrintsAllFields() {
        val factory = FakeCliAutoRunGatewayFactory(
            CliAutoRunStatus(
                enabled = true,
                wifiOnly = true,
                onlyWhileCharging = false,
                constraintsSatisfied = true,
                descriptorCount = 3,
                testCount = 7,
            ),
        )
        val result = runCli("autorun", "status", autoRun = factory)

        assertEquals(0, result.code)
        assertEquals(
            listOf(
                "Autorun: enabled",
                "Wi-Fi only: yes",
                "Only while charging: no",
                "Constraints satisfied: yes",
                "Scheduled: 3 group(s), 7 test(s)",
            ),
            result.stdout,
        )
        assertEquals(1, factory.gateway.statusCalls)
        assertTrue(factory.gateway.closed)
    }

    @Test
    fun statusJsonParsesAndCarriesFields() {
        val factory = FakeCliAutoRunGatewayFactory(
            CliAutoRunStatus(
                enabled = true,
                wifiOnly = false,
                onlyWhileCharging = true,
                constraintsSatisfied = true,
                descriptorCount = 2,
                testCount = 5,
            ),
        )
        val result = runCli("autorun", "status", "--json", autoRun = factory)

        assertEquals(0, result.code)
        val obj = Json.parseToJsonElement(result.stdout.single()).jsonObject
        assertTrue(obj["enabled"]!!.jsonPrimitive.boolean)
        assertEquals(false, obj["wifi_only"]!!.jsonPrimitive.boolean)
        assertTrue(obj["only_while_charging"]!!.jsonPrimitive.boolean)
        assertTrue(obj["constraints_satisfied"]!!.jsonPrimitive.boolean)
        assertEquals(2L, obj["descriptors"]!!.jsonPrimitive.long)
        assertEquals(5L, obj["tests"]!!.jsonPrimitive.long)
        assertTrue(factory.gateway.closed)
    }

    // ---- log show ----

    @Test
    fun logShowReadsTempLogFile() {
        val home = Files.createTempDirectory("ooni-cli-autorun-log")
        val logRuntime = CliRuntime(paths = CliPathLayout.create(ooniHome = home, tempDir = Path.of("/tmp")))
        val logPath = autorunLogPath(logRuntime)
        Files.createDirectories(logPath.parent)
        Files.write(logPath, listOf("2026-08-07 autorun started", "2026-08-07 autorun finished"))

        val result = runCli("autorun", "log", "show", runtime = logRuntime)

        assertEquals(0, result.code)
        assertEquals(
            listOf("2026-08-07 autorun started", "2026-08-07 autorun finished"),
            result.stdout,
        )
    }

    @Test
    fun logShowWithNoFilePrintsDeterministicMessage() {
        val home = Files.createTempDirectory("ooni-cli-autorun-nolog")
        val logRuntime = CliRuntime(paths = CliPathLayout.create(ooniHome = home, tempDir = Path.of("/tmp")))
        val logPath = autorunLogPath(logRuntime)

        val result = runCli("autorun", "log", "show", runtime = logRuntime)

        assertEquals(0, result.code)
        assertEquals(listOf("No autorun log file at $logPath"), result.stdout)
    }

    // ---- log stream ----

    @Test
    fun logStreamPrintsExistingContentsThenFollowsConfiguredPath() {
        val home = Files.createTempDirectory("ooni-cli-autorun-stream")
        val logRuntime = CliRuntime(paths = CliPathLayout.create(ooniHome = home, tempDir = Path.of("/tmp")))
        val logPath = autorunLogPath(logRuntime)
        Files.createDirectories(logPath.parent)
        Files.write(logPath, listOf("existing entry"))
        val follower = RecordingAutorunLogFollower()

        val result = runCli("autorun", "log", "stream", runtime = logRuntime, follower = follower)

        assertEquals(0, result.code)
        assertEquals(listOf("existing entry"), result.stdout)
        // The follower is asked to tail exactly the configured log path (and nothing else).
        assertEquals(logPath, follower.followedPath)
    }

    @Test
    fun logStreamWithNoFileStillFollowsConfiguredPath() {
        val home = Files.createTempDirectory("ooni-cli-autorun-stream-empty")
        val logRuntime = CliRuntime(paths = CliPathLayout.create(ooniHome = home, tempDir = Path.of("/tmp")))
        val logPath = autorunLogPath(logRuntime)
        val follower = RecordingAutorunLogFollower()

        val result = runCli("autorun", "log", "stream", runtime = logRuntime, follower = follower)

        assertEquals(0, result.code)
        assertEquals(listOf("No autorun log file at $logPath"), result.stdout)
        assertEquals(logPath, follower.followedPath)
    }

    private fun runCli(
        vararg args: String,
        runtime: CliRuntime = this.runtime,
        autoRun: FakeCliAutoRunGatewayFactory = FakeCliAutoRunGatewayFactory(defaultStatus()),
        follower: AutorunLogFollower = RecordingAutorunLogFollower(),
    ): AutorunCliResult {
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()
        val code = OoniprobeCli(
            runtime = runtime,
            autoRunGatewayFactory = autoRun,
            autoRunLogFollower = follower,
            input = { null },
        ).run(args.toList().toTypedArray(), stdout::add, stderr::add)
        return AutorunCliResult(code, stdout, stderr)
    }

    private fun defaultStatus() =
        CliAutoRunStatus(
            enabled = false,
            wifiOnly = false,
            onlyWhileCharging = false,
            constraintsSatisfied = false,
            descriptorCount = 0,
            testCount = 0,
        )

    private data class AutorunCliResult(
        val code: Int,
        val stdout: List<String>,
        val stderr: List<String>,
    )

    @Test
    fun followerIsNotInvokedForStatusOrShow() {
        val follower = RecordingAutorunLogFollower()
        runCli("autorun", "status", follower = follower)
        assertNull(follower.followedPath)
    }

    // ---- help (surface discoverability) ----

    @Test
    fun autorunHelpPrintsSubcommandsAndExitsZero() {
        val result = runCli("autorun", "--help")

        assertEquals(0, result.code)
        val usage = result.stdout.single()
        assertTrue(usage.contains("Usage: ooniprobe autorun <command>"), usage)
        listOf("status", "log", "start", "stop").forEach { assertTrue(usage.contains(it), "missing $it in $usage") }
    }

    @Test
    fun autorunLogHelpPrintsSubcommandsAndExitsZero() {
        val result = runCli("autorun", "log", "--help")

        assertEquals(0, result.code)
        val usage = result.stdout.single()
        assertTrue(usage.contains("Usage: ooniprobe autorun log <command>"), usage)
        listOf("show", "stream").forEach { assertTrue(usage.contains(it), "missing $it in $usage") }
    }
}

internal class FakeCliAutoRunGatewayFactory(
    status: CliAutoRunStatus,
    val gateway: FakeCliAutoRunGateway = FakeCliAutoRunGateway(status),
) : CliAutoRunGatewayFactory {
    var creations = 0

    override fun create(runtime: CliRuntime): CliAutoRunGateway {
        creations++
        return gateway
    }
}

internal class FakeCliAutoRunGateway(
    private val status: CliAutoRunStatus,
) : CliAutoRunGateway {
    var statusCalls = 0
    var closed = false

    override suspend fun status(): CliAutoRunStatus {
        statusCalls++
        return status
    }

    override fun close() {
        closed = true
    }
}

internal class RecordingAutorunLogFollower : AutorunLogFollower {
    var followedPath: Path? = null

    override fun follow(
        logPath: Path,
        signals: CliSignals,
        emit: (String) -> Unit,
    ) {
        // Bounded: record the configured path and return immediately (no blocking tail).
        followedPath = logPath
    }
}
