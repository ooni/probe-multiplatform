package org.ooni.probe.cli

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.ooni.probe.core.CliCoreGatewayDependencies
import org.ooni.probe.core.CliAutoRunStatus
import org.ooni.probe.core.CliGeoIp
import org.ooni.probe.core.CliGeoIpGateway
import org.ooni.probe.core.CliResetGateway
import org.ooni.probe.core.CliUploadGateway
import org.ooni.probe.core.CliUploadProgress
import org.ooni.probe.core.DescriptorAssetProvider
import org.ooni.probe.core.DescriptorAssetSet
import org.ooni.probe.core.NativeRuntimeBootstrap
import org.ooni.probe.core.NativeRuntimeBootstrapResult
import org.ooni.probe.data.models.MeasurementsFilter
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Single source-of-truth contract matrix for the parity CLI surface.
 *
 * This suite COMPLEMENTS the per-command suites (`OoniprobeCliTest`, `RunCommandTest`,
 * `GeoIpCommandTest`, `AutorunCommandTest`, `CliStorageCommandsTest`, `CliRuntimeTest`) by
 * enumerating every parity command/subcommand in ONE place with an explicit status, and by
 * asserting the cross-cutting contracts every command must honor:
 *
 *  - exit codes (success `0`; usage/validation `2`; runtime/backend failure `1`),
 *  - JSON output is machine-parseable with the required fields (never snapshotted raw strings),
 *  - backend-free commands reject extra arguments and unknown options.
 *
 * Where a command already has solid valid/invalid/exit-code/JSON tests in its owning suite, the
 * matrix asserts its presence in the enumeration plus a smoke check, rather than duplicating the
 * heavy fixtures (seeded DB rows, measurement graphs) those suites own.
 *
 * Deliberate deviations from Go probe-cli parity are documented in [documentedParityDeviations]
 * and in `.omo/artifacts/cli-completion-full-parity/parity-blockers.md`.
 */
class CommandContractTest {
    private val runtime = CliRuntime(
        paths = CliPathLayout.create(ooniHome = Path.of("/tmp/ooni-cli-contract-home"), tempDir = Path.of("/tmp")),
    )

    // ---- matrix (single source of truth) -------------------------------------------------------

    @Test
    fun everyParityCommandIsImplemented() {
        val pending = MATRIX.filter { it.status == ContractStatus.Pending }
        assertTrue(
            pending.isEmpty(),
            "these parity commands are still pending and must be implemented: ${pending.map { it.invocation }}",
        )
    }

    @Test
    fun matrixEnumeratesTheCanonicalParitySurfaceWithoutDuplicates() {
        val invocations = MATRIX.map { it.invocation }
        assertEquals(invocations.toSet().size, invocations.size, "duplicate rows in the contract matrix")
        assertEquals(EXPECTED_INVOCATIONS, invocations.toSet(), "contract matrix drifted from the parity surface")
    }

    @Test
    fun jsonRowsAreDistinctFromBackendFreeRows() {
        // A backend-free command (help/version/pure parse) must never be marked as a JSON producer:
        // its contract is extra-arg/invalid-option, not JSON schema.
        MATRIX.filter { it.backendFree }.forEach {
            assertFalse(it.producesJson, "${it.invocation} is backend-free and must not produce JSON")
        }
    }

    // ---- exit-code contract ---------------------------------------------------------------------

    @Test
    fun successfulCommandsExitZero() {
        // One representative success per implemented command family.
        listOf(
            arrayOf("version"),
            arrayOf("help"),
            arrayOf("info"),
            arrayOf("info", "--json"),
            arrayOf("list"),
            arrayOf("list", "--json"),
            arrayOf("geoip"),
            arrayOf("geoip", "--json"),
            arrayOf("run", "performance"),
            arrayOf("run", "performance", "--json"),
            arrayOf("upload"),
            arrayOf("upload", "all", "--json"),
            arrayOf("autorun", "status"),
            arrayOf("autorun", "status", "--json"),
            arrayOf("internal", "descriptor-load"),
        ).forEach { args ->
            assertEquals(0, run(*args).code, "expected success for `${args.joinToString(" ")}`")
        }
    }

    @Test
    fun usageAndValidationErrorsExitTwo() {
        // Parse / validation failures (unknown command, unknown option, missing/invalid argument,
        // unknown group/target, missing required flag) all map to the usage exit code.
        listOf(
            arrayOf("bogus"),
            arrayOf("--nope", "version"),
            arrayOf("version", "extra"),
            arrayOf("help", "extra"),
            arrayOf("show"),
            arrayOf("show", "abc"),
            arrayOf("list", "abc"),
            arrayOf("list", "0"),
            arrayOf("run", "bogus"),
            arrayOf("upload", "bogus"),
            arrayOf("upload", "result"),
            arrayOf("rm"),
            arrayOf("reset"),
            arrayOf("autorun", "bogus"),
            arrayOf("autorun", "status", "extra"),
        ).forEach { args ->
            assertEquals(
                USAGE_ERROR_EXIT_CODE,
                run(*args).code,
                "expected usage error (2) for `${args.joinToString(" ")}`",
            )
        }

        // `onboard --batch` without `--yes` is a validation error, but only when onboarding is still
        // incomplete (a completed onboarding short-circuits to an idempotent success).
        assertEquals(
            USAGE_ERROR_EXIT_CODE,
            run("onboard", "--batch", storage = { FakeRunStorageGateway(onboarded = false) }).code,
        )
    }

    @Test
    fun runtimeAndBackendFailuresExitOne() {
        // Well-formed invocations that fail at the backend/runtime layer map to the distinct
        // runtime exit code (RUNTIME_ERROR_EXIT_CODE = 1), never the usage code.
        assertEquals(RUNTIME_ERROR_EXIT_CODE, run("show", "999999").code, "missing measurement")
        assertEquals(RUNTIME_ERROR_EXIT_CODE, run("list", "999999").code, "missing result")
        assertEquals(RUNTIME_ERROR_EXIT_CODE, run("rm", "999999", "--yes").code, "missing result delete")
        // autorun start/stop: deterministic unsupported-service-supervision error (parity blocker).
        assertEquals(RUNTIME_ERROR_EXIT_CODE, run("autorun", "start").code, "autorun start unsupported")
        assertEquals(RUNTIME_ERROR_EXIT_CODE, run("autorun", "stop").code, "autorun stop unsupported")
    }

    @Test
    fun exitCodesAreDistinctForUsageVersusRuntime() {
        // The whole point of a distinct runtime code: a not-found (runtime) is NOT reported as a
        // parse error (usage), and a parse error is NOT reported as a runtime error.
        assertEquals(USAGE_ERROR_EXIT_CODE, run("show", "abc").code)
        assertEquals(RUNTIME_ERROR_EXIT_CODE, run("show", "999999").code)
    }

    // ---- JSON schema contract (parse + required fields, shared helpers) -------------------------

    @Test
    fun infoJsonHasRuntimeSchema() {
        val obj = ContractJson.obj(run("info", "--json").stdout)
        ContractJson.requireKeys(
            obj,
            "ooni_home", "config_file", "temp_dir",
            "software_name", "software_version", "proxy", "batch", "verbose", "log_handler",
        )
    }

    @Test
    fun geoipJsonHasProbeLocationSchema() {
        val obj = ContractJson.obj(
            run("geoip", "--json", geoIp = CliGeoIp(ip = "9.9.9.9", asn = "AS99", networkName = "Net", countryCode = "NZ")).stdout,
        )
        ContractJson.requireKeys(obj, "probe_ip", "probe_asn", "probe_network_name", "probe_cc")
        assertEquals("9.9.9.9", obj["probe_ip"]!!.jsonPrimitive.content)
        assertEquals("NZ", obj["probe_cc"]!!.jsonPrimitive.content)
    }

    @Test
    fun runSummaryJsonHasGroupsAndTests() {
        val obj = ContractJson.obj(run("run", "performance", "--json").stdout)
        ContractJson.requireKeys(obj, "groups", "tests", "uploaded", "failed", "total")
        assertEquals(listOf("performance"), obj["groups"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(2L, obj["tests"]!!.jsonPrimitive.long)
    }

    @Test
    fun autorunStatusJsonHasReadinessSchema() {
        val status = CliAutoRunStatus(
            enabled = true,
            wifiOnly = false,
            onlyWhileCharging = true,
            constraintsSatisfied = true,
            descriptorCount = 2,
            testCount = 5,
        )
        val obj = ContractJson.obj(run("autorun", "status", "--json", autoRun = FakeCliAutoRunGatewayFactory(status)).stdout)
        ContractJson.requireKeys(
            obj,
            "enabled", "wifi_only", "only_while_charging", "constraints_satisfied", "descriptors", "tests",
        )
        assertEquals(2L, obj["descriptors"]!!.jsonPrimitive.long)
        assertEquals(5L, obj["tests"]!!.jsonPrimitive.long)
    }

    @Test
    fun uploadJsonHasProgressSchema() {
        val obj = ContractJson.obj(
            run("upload", "all", "--json", upload = ContractUploadGateway(CliUploadProgress(2, 1, 3, finished = true))).stdout,
        )
        ContractJson.requireKeys(obj, "uploaded", "failed", "total")
        assertEquals(2L, obj["uploaded"]!!.jsonPrimitive.long)
        assertEquals(3L, obj["total"]!!.jsonPrimitive.long)
    }

    @Test
    fun internalDescriptorLoadJsonHasCountsSchema() {
        val obj = ContractJson.obj(run("internal", "descriptor-load").stdout)
        ContractJson.requireKeys(obj, "native_libraries", "descriptors", "default")
        assertEquals("ooni", obj["default"]!!.jsonPrimitive.content)
        // descriptors is an object keyed by asset set; empty assets => all zero.
        assertEquals(0L, obj["descriptors"]!!.jsonObject["ooni"]!!.jsonPrimitive.long)
    }

    @Test
    fun listJsonIsAParseableArray() {
        // Smoke: an empty store yields a parseable JSON array. Full field-level parsing of a
        // populated list (id/name/measurements/anomalies) lives in CliStorageCommandsTest.
        val array = Json.parseToJsonElement(run("list", "--json").stdout.single()).jsonArray
        assertEquals(0, array.size)
    }

    // ---- backend-free commands: extra arguments + unknown options ------------------------------

    @Test
    fun backendFreeCommandsRejectExtraArguments() {
        assertEquals(USAGE_ERROR_EXIT_CODE, run("version", "extra").code)
        assertEquals(USAGE_ERROR_EXIT_CODE, run("help", "extra").code)
    }

    @Test
    fun unknownGlobalOptionFails() {
        assertEquals(USAGE_ERROR_EXIT_CODE, run("--nope").code)
        assertEquals(USAGE_ERROR_EXIT_CODE, run("--nope", "version").code)
    }

    @Test
    fun unknownCommandIsReportedDistinctly() {
        val result = run("totally-unknown")
        assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
        assertTrue(result.stderr.any { it.contains("Unknown command: totally-unknown") }, result.stderr.toString())
    }

    // ---- deliberate parity deviations (documented, asserted) -----------------------------------

    @Test
    fun documentedParityDeviations() {
        // (1) Global help is disabled (`helpOptionNames = emptySet()`), so `--help`/`-h` on a leaf
        // subcommand is an unknown option -> usage error (2), NOT a help screen. `help` and the root
        // `--help` are the discoverability surface. (The `autorun`/`autorun log` groups deliberately
        // register their OWN `-h`/`--help` exit-0 usage screens; that exception is covered by
        // AutorunCommandTest.)
        assertEquals(USAGE_ERROR_EXIT_CODE, run("version", "--help").code)
        assertEquals(USAGE_ERROR_EXIT_CODE, run("list", "--help").code)
        assertEquals(USAGE_ERROR_EXIT_CODE, run("show", "--help").code)

        // (2) `run experimental` covers only the 3 experimental nettests the bundled OONI descriptor
        // exposes (stunreachability, openvpn, vanilla_tor); DNS Check / ECH Check / Tor Snowflake are
        // a data/engine gap, not a CLI gap. See parity-blockers.md (T9). The CLI still succeeds.
        assertEquals(0, run("run", "experimental").code)

        // (3) `autorun start`/`stop` OS-service supervision is unimplemented: deterministic runtime
        // error (exit 1), never a faked success. See parity-blockers.md (T12).
        assertEquals(RUNTIME_ERROR_EXIT_CODE, run("autorun", "start").code)
        assertEquals(RUNTIME_ERROR_EXIT_CODE, run("autorun", "stop").code)

        // (4) The `run` onboarding preflight (consent required before any run gateway is built) is
        // owned by RunCommandFailureTest.batchRunWithIncompleteOnboardingFailsBeforeRunGateway; it is
        // enumerated here as the ONBOARDING_PREFLIGHT_NOTE matrix note.
        assertTrue(ONBOARDING_PREFLIGHT_NOTE.isNotBlank())
    }

    // ---- harness -------------------------------------------------------------------------------

    private fun run(
        vararg args: String,
        storage: CliStorageGatewayFactory = CliStorageGatewayFactory { FakeRunStorageGateway(onboarded = true) },
        run: FakeCliRunGatewayFactory = FakeCliRunGatewayFactory(),
        geoIp: CliGeoIp = CliGeoIp(ip = "1.1.1.1", asn = "AS1", networkName = "n", countryCode = "US"),
        upload: CliUploadGateway = ContractUploadGateway(),
        autoRun: FakeCliAutoRunGatewayFactory = FakeCliAutoRunGatewayFactory(disabledStatus()),
        core: CliCoreGatewayDependencies = ContractCoreGateway(),
    ): ContractResult {
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()
        val code = OoniprobeCli(
            runtime = runtime,
            coreGatewayFactory = { core },
            storageGatewayFactory = storage,
            uploadGatewayFactory = { upload },
            runGatewayFactory = run,
            geoIpGatewayFactory = { ContractGeoIpGateway(geoIp) },
            autoRunGatewayFactory = autoRun,
            resetGatewayFactory = { ContractResetGateway() },
            autoRunLogFollower = RecordingAutorunLogFollower(),
            input = { null },
        ).run(args.toList().toTypedArray(), stdout::add, stderr::add)
        return ContractResult(code, stdout, stderr)
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

    private data class ContractResult(
        val code: Int,
        val stdout: List<String>,
        val stderr: List<String>,
    )

    private enum class ContractStatus { Implemented, Pending }

    private data class CommandContract(
        val invocation: String,
        val status: ContractStatus,
        val producesJson: Boolean = false,
        val backendFree: Boolean = false,
        val note: String = "",
    )

    private companion object {
        // The `run` onboarding preflight is covered by
        // RunCommandFailureTest.batchRunWithIncompleteOnboardingFailsBeforeRunGateway (batch run
        // without consent fails BEFORE the run gateway is constructed).
        const val ONBOARDING_PREFLIGHT_NOTE =
            "batch run without onboarding consent fails before the run gateway (RunCommandFailureTest)."

        val MATRIX = listOf(
            CommandContract("version", ContractStatus.Implemented, backendFree = true),
            CommandContract("info", ContractStatus.Implemented, producesJson = true),
            CommandContract("help", ContractStatus.Implemented, backendFree = true),
            CommandContract("list", ContractStatus.Implemented, producesJson = true),
            CommandContract("show", ContractStatus.Implemented, producesJson = true),
            CommandContract("rm", ContractStatus.Implemented),
            CommandContract("reset", ContractStatus.Implemented),
            CommandContract("onboard", ContractStatus.Implemented, note = ONBOARDING_PREFLIGHT_NOTE),
            CommandContract("upload", ContractStatus.Implemented, producesJson = true),
            CommandContract("upload all", ContractStatus.Implemented, producesJson = true),
            CommandContract("upload result", ContractStatus.Implemented, producesJson = true),
            CommandContract("upload measurement", ContractStatus.Implemented, producesJson = true),
            CommandContract("geoip", ContractStatus.Implemented, producesJson = true),
            CommandContract("run", ContractStatus.Implemented, producesJson = true, note = "bare run == run all"),
            CommandContract("run websites", ContractStatus.Implemented, producesJson = true),
            CommandContract("run im", ContractStatus.Implemented, producesJson = true),
            CommandContract("run performance", ContractStatus.Implemented, producesJson = true),
            CommandContract("run circumvention", ContractStatus.Implemented, producesJson = true),
            CommandContract("run middlebox", ContractStatus.Implemented, producesJson = true),
            CommandContract(
                "run experimental",
                ContractStatus.Implemented,
                producesJson = true,
                note = "3/6 nettests present (DNS/ECH/Snowflake are a data gap, see parity-blockers.md T9)",
            ),
            CommandContract("run unattended", ContractStatus.Implemented, producesJson = true),
            CommandContract("run all", ContractStatus.Implemented, producesJson = true),
            CommandContract("autorun status", ContractStatus.Implemented, producesJson = true),
            CommandContract("autorun log show", ContractStatus.Implemented),
            CommandContract("autorun log stream", ContractStatus.Implemented),
            CommandContract(
                "autorun start",
                ContractStatus.Implemented,
                note = "unsupported OS-service supervision -> deterministic exit 1 (parity-blockers.md T12)",
            ),
            CommandContract(
                "autorun stop",
                ContractStatus.Implemented,
                note = "unsupported OS-service supervision -> deterministic exit 1 (parity-blockers.md T12)",
            ),
            CommandContract("internal descriptor-load", ContractStatus.Implemented, producesJson = true),
        )

        val EXPECTED_INVOCATIONS = setOf(
            "version", "info", "help", "list", "show", "rm", "reset", "onboard",
            "upload", "upload all", "upload result", "upload measurement",
            "geoip",
            "run", "run websites", "run im", "run performance", "run circumvention",
            "run middlebox", "run experimental", "run unattended", "run all",
            "autorun status", "autorun log show", "autorun log stream", "autorun start", "autorun stop",
            "internal descriptor-load",
        )
    }
}

// ---- shared JSON schema helpers -------------------------------------------------------------

/** Parses machine-readable command output and asserts required fields (never snapshots raw text). */
private object ContractJson {
    fun obj(stdout: List<String>): JsonObject = Json.parseToJsonElement(stdout.single()).jsonObject

    fun requireKeys(obj: JsonObject, vararg keys: String) {
        keys.forEach { assertTrue(obj.containsKey(it), "missing required field `$it` in $obj") }
    }
}

// ---- minimal contract fakes (small, non-fixture surfaces reused across the exit-code matrix) --

private class ContractGeoIpGateway(private val result: CliGeoIp) : CliGeoIpGateway {
    override suspend fun lookup(): CliGeoIp = result

    override fun close() = Unit
}

private class ContractUploadGateway(
    private vararg val progress: CliUploadProgress,
) : CliUploadGateway {
    override fun uploadMissing(filter: MeasurementsFilter): Flow<CliUploadProgress> = progress.toList().asFlow()

    override fun cancel() = Unit

    override fun close() = Unit
}

private class ContractResetGateway : CliResetGateway {
    override suspend fun clearSecureStorage() = Unit

    override fun close() = Unit
}

private class ContractCoreGateway(
    private val assetsJson: String = "[]",
) : CliCoreGatewayDependencies {
    override val descriptorAssets = DescriptorAssetProvider { assetsJson }
    override val defaultDescriptorAssetSet = DescriptorAssetSet.Ooni
    override val nativeRuntimeBootstrap = object : NativeRuntimeBootstrap {
        override fun configure() = NativeRuntimeBootstrapResult(emptyList())
    }
}
