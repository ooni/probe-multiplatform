package org.ooni.probe.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.ooni.probe.core.CliEngineConfig
import org.ooni.probe.core.CliUploadGateway
import org.ooni.probe.core.CliUploadProgress
import org.ooni.probe.core.buildDesktopCliUploadGateway
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.probe.data.models.ResultModel
import java.nio.file.Files

fun interface CliUploadGatewayFactory {
    fun create(runtime: CliRuntime): CliUploadGateway
}

internal object ProductionCliUploadGatewayFactory : CliUploadGatewayFactory {
    override fun create(runtime: CliRuntime): CliUploadGateway {
        Files.createDirectories(runtime.paths.dataDir)
        return buildDesktopCliUploadGateway(
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

    private const val OONI_API_BASE_URL = "https://api.ooni.io"
}

internal class UploadCommand(
    private val output: CliOutput,
    private val uploadGatewayFactory: CliUploadGatewayFactory,
    private val signals: CliSignals,
) : CliktCommand(name = "upload") {
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()
    private val target by argument("target").multiple()

    // Accepted for probe-cli parity but a no-op here: the CLI upload path is anonymous-only (it
    // submits through the engine's submit call and never prepares/registers user credentials nor
    // reads/writes secure-storage credentials), so `--no-creds` is already satisfied unconditionally.
    @Suppress("unused")
    private val noCreds by option("--no-creds").flag()

    override fun run() {
        val filter = resolveFilter(target)
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        val gateway = uploadGatewayFactory.create(runtime)
        // A SIGINT/SIGTERM cancels the in-flight upload collection; the flow completes gracefully
        // (driver close still happens in finally) and we surface exit code 130 below.
        signals.setActive { gateway.cancel() }
        try {
            runBlocking {
                var last: CliUploadProgress? = null
                gateway.uploadMissing(filter).collect { last = it }
                emit(runtime, last)
            }
        } finally {
            signals.clearActive()
            gateway.close()
        }
        if (signals.wasSignalled()) throw ProgramResult(SIGINT_EXIT_CODE)
    }

    private fun emit(runtime: CliRuntime, progress: CliUploadProgress?) {
        val uploaded = progress?.uploaded ?: 0
        val failed = progress?.failedToUpload ?: 0
        val total = progress?.total ?: 0
        if (runtime.jsonOutput) {
            output.stdout(
                CliJson.obj(
                    "uploaded" to CliJson.num(uploaded.toLong()),
                    "failed" to CliJson.num(failed.toLong()),
                    "total" to CliJson.num(total.toLong()),
                ),
            )
        } else if (total == 0) {
            output.stdout("No measurements to upload.")
        } else {
            output.stdout("Uploaded $uploaded/$total ($failed failed).")
        }
    }

    private fun resolveFilter(args: List<String>): MeasurementsFilter =
        when {
            args.isEmpty() -> MeasurementsFilter.All
            args.size == 1 && args[0].equals("all", ignoreCase = true) -> MeasurementsFilter.All
            args.size == 2 && args[0].equals("result", ignoreCase = true) ->
                MeasurementsFilter.Result(ResultModel.Id(parseId(args[1], "result id")))
            args.size == 2 && args[0].equals("measurement", ignoreCase = true) ->
                MeasurementsFilter.Measurement(MeasurementModel.Id(parseId(args[1], "measurement id")))
            else -> throw CliRuntimeValidationException("Usage: upload [all | result <id> | measurement <id>]")
        }

    private fun parseId(value: String, label: String): Long {
        val id = value.toLongOrNull() ?: throw CliRuntimeValidationException("Invalid $label: $value")
        if (id <= 0) throw CliRuntimeValidationException("Invalid $label: $value")
        return id
    }
}
