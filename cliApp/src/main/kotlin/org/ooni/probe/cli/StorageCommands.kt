package org.ooni.probe.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
import kotlinx.coroutines.runBlocking
import org.ooni.probe.core.CliCoreGatewayDependencies
import org.ooni.probe.core.CliEngineConfig
import org.ooni.probe.core.CliResetGateway
import org.ooni.probe.core.CliStorageConfig
import org.ooni.probe.core.CliStorageGateway
import org.ooni.probe.core.DescriptorAssetSet
import org.ooni.probe.core.DesktopCliGatewayFactory
import org.ooni.probe.core.buildDesktopCliStorageGateway
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementWithUrl
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.ResultWithNetworkAndAggregates
import java.nio.file.Files
import java.nio.file.Path

fun interface CliStorageGatewayFactory {
    fun create(runtime: CliRuntime): CliStorageGateway
}

internal object ProductionCliStorageGatewayFactory : CliStorageGatewayFactory {
    override fun create(runtime: CliRuntime): CliStorageGateway {
        Files.createDirectories(runtime.paths.dataDir)
        return buildDesktopCliStorageGateway(
            CliStorageConfig(
                databaseDir = runtime.paths.dataDir.toString(),
                preferencesFile = runtime.paths.preferenceDataStoreFile.toString(),
            ),
        )
    }
}

// ---- list ----------------------------------------------------------------------------------

internal class ListCommand(
    private val output: CliOutput,
    private val storageGatewayFactory: CliStorageGatewayFactory,
) : CliktCommand(name = "list") {
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()
    private val resultId by argument("resultId").long().optional()

    override fun run() {
        val id = resultId
        if (id != null) requirePositive(id, "result id")
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        val gateway = storageGatewayFactory.create(runtime)
        try {
            runBlocking {
                if (id == null) {
                    emitResults(output, runtime, gateway.listResults())
                } else {
                    if (!gateway.resultExists(ResultModel.Id(id))) output.fail("Result not found: $id", RUNTIME_ERROR_EXIT_CODE)
                    emitMeasurements(output, runtime, gateway.listMeasurements(ResultModel.Id(id)))
                }
            }
        } finally {
            gateway.close()
        }
    }
}

// ---- show ----------------------------------------------------------------------------------

internal class ShowCommand(
    private val output: CliOutput,
    private val storageGatewayFactory: CliStorageGatewayFactory,
) : CliktCommand(name = "show") {
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()
    private val measurementId by argument("measurementId").long()

    override fun run() {
        requirePositive(measurementId, "measurement id")
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        val gateway = storageGatewayFactory.create(runtime)
        try {
            runBlocking {
                val measurement = gateway.getMeasurement(MeasurementModel.Id(measurementId))
                    ?: output.fail("Measurement not found: $measurementId", RUNTIME_ERROR_EXIT_CODE)
                output.stdout(measurementDetailJson(measurement))
            }
        } finally {
            gateway.close()
        }
    }
}

// ---- rm ------------------------------------------------------------------------------------

internal class RemoveCommand(
    private val output: CliOutput,
    private val storageGatewayFactory: CliStorageGatewayFactory,
    private val input: () -> String?,
) : CliktCommand(name = "rm") {
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()
    private val resultId by argument("resultId").long().optional()
    private val all by option("--all").flag()
    private val yes by option("-y", "--yes").flag()

    override fun run() {
        val id = resultId
        if (id != null && all) throw CliRuntimeValidationException("rm accepts either a result id or --all, not both")
        if (id == null && !all) throw CliRuntimeValidationException("rm requires a result id or --all")
        if (id != null) requirePositive(id, "result id")
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        confirmOrAbort(runtime, if (all) "Delete ALL results?" else "Delete result $id?")
        val gateway = storageGatewayFactory.create(runtime)
        try {
            runBlocking {
                if (all) {
                    gateway.deleteAllResults()
                    output.stdout("Deleted all results.")
                } else if (gateway.deleteResult(ResultModel.Id(id!!))) {
                    output.stdout("Deleted result $id.")
                } else {
                    output.fail("Result not found: $id", RUNTIME_ERROR_EXIT_CODE)
                }
            }
        } finally {
            gateway.close()
        }
    }

    private fun confirmOrAbort(runtime: CliRuntime, prompt: String) {
        if (yes) return
        if (runtime.batch) throw CliRuntimeValidationException("Refusing to delete without --yes in batch mode")
        output.stdout("$prompt [y/N]")
        val answer = input()?.trim()?.lowercase()
        if (answer != "y" && answer != "yes") {
            output.stdout("Aborted.")
            throw ProgramResult(0)
        }
    }
}

// ---- reset ---------------------------------------------------------------------------------

fun interface CliResetGatewayFactory {
    fun create(runtime: CliRuntime): CliResetGateway
}

internal object ProductionCliResetGatewayFactory : CliResetGatewayFactory {
    private const val OONI_API_BASE_URL = "https://api.ooni.io"

    override fun create(runtime: CliRuntime): CliResetGateway =
        // Wire through DesktopCliGatewayFactory so the CLI never constructs a platform SecureStorage itself.
        // Only osName/baseSoftwareName drive secure-storage scoping; the remaining fields mirror the
        // other engine gateways' config for consistency.
        DesktopCliGatewayFactory.createCliResetGateway(
            CliEngineConfig(
                databaseDir = runtime.paths.dataDir.toString(),
                baseFileDir = runtime.paths.dataDir.toString(),
                cacheDir = runtime.paths.cacheDir.toString(),
                ooniApiBaseUrl = OONI_API_BASE_URL,
                baseSoftwareName = "ooniprobe",
                softwareVersion = CliBuildConfig.VERSION_NAME,
                passportVersion = CliBuildConfig.VERSION_NAME,
                proxy = runtime.proxy,
                osName = System.getProperty("os.name") ?: "unknown",
                osVersion = System.getProperty("os.version") ?: "unknown",
            ),
        )
}

internal class ResetCommand(
    private val output: CliOutput,
    private val storageGatewayFactory: CliStorageGatewayFactory,
    private val resetGatewayFactory: CliResetGatewayFactory,
    @Suppress("unused") private val input: () -> String?,
) : CliktCommand(name = "reset") {
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()
    private val force by option("--force").flag()

    override fun run() {
        if (!force) throw CliRuntimeValidationException("reset requires --force")
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        val home = runtime.paths.ooniHome.toAbsolutePath().normalize()
        assertSafeHome(home)

        // Ordering (probe-cli parity): (1) close DB/core resources, (2) clear scoped secure storage,
        // (3) delete filesystem paths. If secure-storage clearing fails we abort BEFORE deleting any
        // file so a failed clear never leaves the on-disk data wiped.

        // (1) Release any DB/core resources so no driver holds probe.db open when we delete it.
        // Best-effort: a corrupt or locked database must not block the reset that removes it.
        runCatching { storageGatewayFactory.create(runtime).close() }

        // (2) Clear the OONI-scoped secure storage; abort deletion on failure.
        val resetGateway = resetGatewayFactory.create(runtime)
        try {
            runBlocking { resetGateway.clearSecureStorage() }
        } catch (error: Exception) {
            output.fail(
                "Failed to clear secure storage; aborting reset: ${error.message}",
                RUNTIME_ERROR_EXIT_CODE,
            )
        } finally {
            resetGateway.close()
        }

        // (3) Delete only OONI-owned filesystem paths.
        var removed = 0
        runtime.paths.resetDeletionSet().forEach { target ->
            val canonical = target.toAbsolutePath().normalize()
            // resetDeletionSet only returns OONI-owned paths; re-check containment defensively.
            if (canonical.startsWith(home) && canonical != home.root) {
                val file = canonical.toFile()
                if (file.exists() && file.deleteRecursively()) removed++
            }
        }
        output.stdout("Reset complete. Removed $removed path(s) under $home.")
        output.stdout("Secure storage cleared.")
    }

    private fun assertSafeHome(home: Path) {
        val userHome = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize()
        if (home == home.root || home == userHome || home.nameCount == 0) {
            output.fail("Refusing to reset unsafe OONI home: $home", RUNTIME_ERROR_EXIT_CODE)
        }
    }
}

// ---- onboard -------------------------------------------------------------------------------

internal class OnboardCommand(
    private val output: CliOutput,
    private val storageGatewayFactory: CliStorageGatewayFactory,
    private val input: () -> String?,
) : CliktCommand(name = "onboard") {
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()
    private val yes by option("-y", "--yes").flag()

    override fun run() {
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        val gateway = storageGatewayFactory.create(runtime)
        try {
            runBlocking {
                if (gateway.isOnboardingComplete()) {
                    output.stdout("Onboarding already complete.")
                    return@runBlocking
                }
                if (!yes) {
                    if (runtime.batch) {
                        throw CliRuntimeValidationException("onboard requires --yes in batch mode")
                    }
                    output.stdout(CONSENT_TEXT)
                    output.stdout("Type 'yes' to accept:")
                    val answer = input()?.trim()?.lowercase()
                    if (answer != "y" && answer != "yes") {
                        output.stdout("Onboarding not completed.")
                        throw ProgramResult(RUNTIME_ERROR_EXIT_CODE)
                    }
                }
                gateway.completeOnboarding()
                output.stdout("Onboarding complete.")
            }
        } finally {
            gateway.close()
        }
    }

    private companion object {
        val CONSENT_TEXT =
            """
            OONI Probe collects and publishes network measurement data to investigate internet
            censorship. Measurements may include your network's IP/ASN and are published openly.
            See https://ooni.org/about/data-policy/ for details.
            """.trimIndent()
    }
}

// ---- internal (hidden diagnostics) ---------------------------------------------------------

internal class InternalCommand(
    output: CliOutput,
    coreGatewayFactory: CliCoreGatewayFactory,
) : CliktCommand(name = "internal") {
    init {
        subcommands(DescriptorLoadCommand(output, coreGatewayFactory))
    }

    override fun run() = Unit
}

internal class DescriptorLoadCommand(
    private val output: CliOutput,
    private val coreGatewayFactory: CliCoreGatewayFactory,
) : CliktCommand(name = "descriptor-load") {
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()

    override fun run() {
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        val gateway = coreGatewayFactory.create(runtime) as? CliCoreGatewayDependencies
            ?: output.fail("Descriptor gateway unavailable", RUNTIME_ERROR_EXIT_CODE)
        runBlocking {
            val native = gateway.nativeRuntimeBootstrap.configure().appliedLibraries
            val counts = DescriptorAssetSet.entries.associateWith { gateway.loadDescriptors(it).size }
            output.stdout(
                CliJson.obj(
                    "native_libraries" to CliJson.arr(native.map { CliJson.str(it) }),
                    "descriptors" to CliJson.obj(
                        *counts.map { (set, count) -> set.name.lowercase() to CliJson.num(count.toLong()) }
                            .toTypedArray(),
                    ),
                    "default" to CliJson.str(gateway.defaultDescriptorAssetSet.name.lowercase()),
                ),
            )
        }
    }
}

// ---- shared helpers ------------------------------------------------------------------------

private fun CliOutput.fail(message: String, code: Int): Nothing {
    stderr(message)
    throw ProgramResult(code)
}

private fun requirePositive(id: Long, label: String) {
    if (id <= 0) throw CliRuntimeValidationException("Invalid $label: $id")
}

private fun emitResults(
    output: CliOutput,
    runtime: CliRuntime,
    results: List<ResultWithNetworkAndAggregates>,
) {
    if (runtime.jsonOutput) {
        output.stdout(CliJson.arr(results.map(::resultJson)))
        return
    }
    if (results.isEmpty()) {
        output.stdout("No results.")
        return
    }
    results.forEach { output.stdout(resultLine(it)) }
}

private fun resultJson(row: ResultWithNetworkAndAggregates): String =
    CliJson.obj(
        "id" to CliJson.numOrNull(row.result.id?.value),
        "name" to CliJson.str(row.result.descriptorName),
        "start_time" to CliJson.str(row.result.startTime.toString()),
        "task_origin" to CliJson.str(row.result.taskOrigin.value),
        "network_name" to CliJson.str(row.network?.name),
        "asn" to CliJson.str(row.network?.asn),
        "country_code" to CliJson.str(row.network?.countryCode),
        "is_done" to CliJson.bool(row.result.isDone),
        "measurements" to CliJson.num(row.measurementCounts.done),
        "anomalies" to CliJson.num(row.measurementCounts.anomaly),
        "failures" to CliJson.num(row.measurementCounts.failed),
        "all_uploaded" to CliJson.bool(row.allMeasurementsUploaded),
    )

private fun resultLine(row: ResultWithNetworkAndAggregates): String {
    val id = row.result.id?.value ?: 0
    val network = row.network?.let { net ->
        listOfNotNull(net.asn, net.name, net.countryCode?.let { "($it)" }).joinToString(" ")
    }?.takeIf { it.isNotBlank() } ?: "unknown network"
    val counts = "${row.measurementCounts.done} done, ${row.measurementCounts.anomaly} anomaly"
    val upload = if (row.allMeasurementsUploaded) "uploaded" else "not fully uploaded"
    return "#$id  ${row.result.descriptorName ?: "(unnamed)"}  $network  $counts  $upload"
}

private fun emitMeasurements(
    output: CliOutput,
    runtime: CliRuntime,
    measurements: List<MeasurementWithUrl>,
) {
    if (runtime.jsonOutput) {
        output.stdout(CliJson.arr(measurements.map(::measurementRowJson)))
        return
    }
    if (measurements.isEmpty()) {
        output.stdout("No measurements.")
        return
    }
    measurements.forEach { output.stdout(measurementLine(it)) }
}

private fun measurementRowJson(row: MeasurementWithUrl): String =
    CliJson.obj(
        "id" to CliJson.numOrNull(row.measurement.id?.value),
        "test_name" to CliJson.str(row.measurement.test.name),
        "url" to CliJson.str(row.url?.url),
        "report_id" to CliJson.str(row.measurement.reportId?.value),
        "is_uploaded" to CliJson.bool(row.measurement.isUploaded),
        "is_anomaly" to CliJson.bool(row.measurement.isAnomaly),
        "is_failed" to CliJson.bool(row.measurement.isFailed),
    )

private fun measurementLine(row: MeasurementWithUrl): String {
    val id = row.measurement.id?.value ?: 0
    val flags = buildList {
        if (row.measurement.isAnomaly) add("anomaly")
        if (row.measurement.isFailed) add("failed")
        add(if (row.measurement.isUploaded) "uploaded" else "not uploaded")
    }.joinToString(", ")
    val url = row.url?.url?.let { "  $it" } ?: ""
    return "#$id  ${row.measurement.test.name}$url  [$flags]"
}

private fun measurementDetailJson(row: MeasurementWithUrl): String {
    val measurement = row.measurement
    return CliJson.obj(
        "id" to CliJson.numOrNull(measurement.id?.value),
        "test_name" to CliJson.str(measurement.test.name),
        "result_id" to CliJson.num(measurement.resultId.value),
        "report_id" to CliJson.str(measurement.reportId?.value),
        "uid" to CliJson.str(measurement.uid?.value),
        "is_uploaded" to CliJson.bool(measurement.isUploaded),
        "is_anomaly" to CliJson.bool(measurement.isAnomaly),
        "is_failed" to CliJson.bool(measurement.isFailed),
        "start_time" to CliJson.str(measurement.startTime?.toString()),
        "url" to CliJson.str(row.url?.url),
        "test_keys" to CliJson.rawOrNull(measurement.testKeys),
    )
}
