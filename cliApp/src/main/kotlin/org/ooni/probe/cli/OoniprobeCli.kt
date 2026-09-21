package org.ooni.probe.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.eagerOption
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import org.ooni.probe.core.DesktopCliGatewayFactory

typealias CliCoreGateway = org.ooni.probe.core.CliCoreGateway

internal const val USAGE_ERROR_EXIT_CODE = 2
internal const val RUNTIME_ERROR_EXIT_CODE = 1

class OoniprobeCli(
    private val runtime: CliRuntime,
    private val coreGatewayFactory: CliCoreGatewayFactory = ProductionCliCoreGatewayFactory,
    private val storageGatewayFactory: CliStorageGatewayFactory = ProductionCliStorageGatewayFactory,
    private val uploadGatewayFactory: CliUploadGatewayFactory = ProductionCliUploadGatewayFactory,
    private val runGatewayFactory: CliRunGatewayFactory = ProductionCliRunGatewayFactory,
    private val geoIpGatewayFactory: CliGeoIpGatewayFactory = ProductionCliGeoIpGatewayFactory,
    private val autoRunGatewayFactory: CliAutoRunGatewayFactory = ProductionCliAutoRunGatewayFactory,
    private val resetGatewayFactory: CliResetGatewayFactory = ProductionCliResetGatewayFactory,
    private val autoRunLogFollower: AutorunLogFollower = ProductionAutorunLogFollower,
    private val input: () -> String? = { readlnOrNull() },
    private val signals: CliSignals = CliSignals(),
) {
    fun run(
        args: Array<String>,
        stdout: (String) -> Unit,
        stderr: (String) -> Unit,
    ): Int {
        val output = CliOutput(stdout, stderr)
        val command = OoniprobeCommand(
            runtime,
            coreGatewayFactory,
            storageGatewayFactory,
            uploadGatewayFactory,
            runGatewayFactory,
            geoIpGatewayFactory,
            autoRunGatewayFactory,
            resetGatewayFactory,
            autoRunLogFollower,
            output,
            input,
            signals,
        )

        return try {
            command.parse(args)
            0
        } catch (result: ProgramResult) {
            result.statusCode
        } catch (error: CliktError) {
            output.stderr(errorMessage(args, error))
            output.stderr(OoniprobeCommand.usage())
            USAGE_ERROR_EXIT_CODE
        } catch (error: CliRuntimeValidationException) {
            output.stderr(error.message ?: "Usage error")
            output.stderr(OoniprobeCommand.usage())
            USAGE_ERROR_EXIT_CODE
        } catch (error: Exception) {
            output.stderr(error.message ?: "Unexpected error: ${error::class.simpleName}")
            RUNTIME_ERROR_EXIT_CODE
        }
    }

    internal fun createCoreGateway(): CliCoreGateway = coreGatewayFactory.create(runtime)

    private fun errorMessage(args: Array<String>, error: CliktError): String {
        val candidate = args.firstOrNull()
        return if (candidate != null && !candidate.startsWith("-") && candidate !in COMMAND_NAMES) {
            "Unknown command: $candidate"
        } else {
            error.message ?: "Usage error"
        }
    }

    private companion object {
        val COMMAND_NAMES = setOf(
            "version", "info", "geoip", "help", "list", "show", "rm", "reset", "onboard", "run", "upload", "autorun",
            "internal",
        )
    }
}

fun interface CliCoreGatewayFactory {
    fun create(runtime: CliRuntime): CliCoreGateway
}

internal object ProductionCliCoreGatewayFactory : CliCoreGatewayFactory {
    override fun create(runtime: CliRuntime): CliCoreGateway = DesktopCliGatewayFactory.createCliGateway()
}

internal class CliOutput(
    val stdout: (String) -> Unit,
    val stderr: (String) -> Unit,
)

internal class OoniprobeCommand(
    private val runtime: CliRuntime,
    private val coreGatewayFactory: CliCoreGatewayFactory,
    storageGatewayFactory: CliStorageGatewayFactory,
    uploadGatewayFactory: CliUploadGatewayFactory,
    runGatewayFactory: CliRunGatewayFactory,
    geoIpGatewayFactory: CliGeoIpGatewayFactory,
    autoRunGatewayFactory: CliAutoRunGatewayFactory,
    resetGatewayFactory: CliResetGatewayFactory,
    autoRunLogFollower: AutorunLogFollower,
    private val output: CliOutput,
    input: () -> String?,
    signals: CliSignals,
) : CliktCommand(name = "ooniprobe") {
    override val invokeWithoutSubcommand = true
    private val runtimeOptions by CliRuntimeOptionGroup()

    init {
        context {
            helpOptionNames = emptySet()
        }
        eagerOption("-h", "--help") {
            output.stdout(usage())
            throw ProgramResult(0)
        }
        eagerOption("-V", "--version") {
            output.stdout(version())
            throw ProgramResult(0)
        }
        subcommands(
            VersionCommand(output),
            InfoCommand(output),
            GeoIpCommand(output, geoIpGatewayFactory),
            HelpCommand(output),
            ListCommand(output, storageGatewayFactory),
            ShowCommand(output, storageGatewayFactory),
            RemoveCommand(output, storageGatewayFactory, input),
            ResetCommand(output, storageGatewayFactory, resetGatewayFactory, input),
            OnboardCommand(output, storageGatewayFactory, input),
            RunCommand(output, runGatewayFactory, storageGatewayFactory, signals),
            UploadCommand(output, uploadGatewayFactory, signals),
            AutorunCommand(output, autoRunGatewayFactory, autoRunLogFollower, signals),
            InternalCommand(output, coreGatewayFactory),
        )
    }

    override fun run() {
        val context = CliRuntimeContext(runtime, runtimeOptions.asInvocation())
        context.resolve()
        currentContext.obj = context
        if (currentContext.invokedSubcommand == null) {
            output.stdout(usage())
        }
    }

    internal fun createCoreGateway(): CliCoreGateway = coreGatewayFactory.create(runtime)

    companion object {
        fun usage() = """
            Usage: ooniprobe <command>

            Commands:
              version   Print CLI version
              info      Print local runtime paths
              geoip     Print the probe's current network location
              list      List results, or measurements for a result
              show      Print a stored measurement as JSON
              rm        Delete results
              reset     Delete all local OONI Probe data
              onboard   Accept the informed-consent onboarding
              run       Run measurement groups
              upload    Upload measurements not yet submitted
              autorun   Inspect autorun status and logs
              internal  Hidden diagnostics
              help      Print this help
        """.trimIndent()

        fun version() = "OONI Probe ${CliBuildConfig.VERSION_NAME} (${CliBuildConfig.VERSION_CODE})"
    }
}

private class VersionCommand(
    private val output: CliOutput,
) : CliktCommand(name = "version") {
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()

    override fun run() {
        runtimeContext.resolve(runtimeOptions.asInvocation())
        output.stdout(OoniprobeCommand.version())
    }
}

private class InfoCommand(
    private val output: CliOutput,
) : CliktCommand(name = "info") {
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()

    override fun run() {
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        if (runtime.jsonOutput) {
            output.stdout(runtime.infoJson())
            return
        }
        output.stdout("OONI home: ${runtime.ooniHome}")
        output.stdout("Temp dir: ${runtime.tempDir}")
    }
}

private class HelpCommand(
    private val output: CliOutput,
) : CliktCommand(name = "help") {
    override fun run() {
        output.stdout(OoniprobeCommand.usage())
    }
}

internal class CliRuntimeOptionGroup : OptionGroup("Runtime options") {
    private val configFile by option("-c", "--config").path(mustExist = false, canBeDir = false)
    private val softwareName by option("--software-name")
    private val softwareVersion by option("--software-version")
    private val proxy by option("--proxy")
    private val batch by option("--batch").flag()
    private val verbose by option("-v", "--verbose").flag()
    private val logHandler by option("--log-handler")
    private val jsonOutput by option("--json").flag()

    fun asInvocation() = CliInvocationOptions(
        configFile = configFile,
        softwareName = softwareName,
        softwareVersion = softwareVersion,
        proxy = proxy,
        batch = batch.takeIf { it },
        verbose = verbose.takeIf { it },
        logHandler = logHandler?.let(::parseLogHandler),
        jsonOutput = jsonOutput.takeIf { it },
    )

    private fun parseLogHandler(value: String): CliLogHandler =
        CliLogHandler.entries.firstOrNull { it.value == value }
            ?: throw CliRuntimeValidationException("Invalid log handler: $value")
}

internal class CliRuntimeContext(
    private val baseRuntime: CliRuntime,
    private val rootOptions: CliInvocationOptions,
) {
    fun resolve(commandOptions: CliInvocationOptions = CliInvocationOptions()): CliRuntime =
        baseRuntime.withInvocation(rootOptions.merge(commandOptions))
}

private fun CliRuntime.infoJson(): String =
    """{"ooni_home":${jsonString(paths.ooniHome.toString())},"config_file":${jsonString(paths.configFile.toString())},"temp_dir":${jsonString(paths.tempDir.toString())},"software_name":${jsonString(softwareName)},"software_version":${jsonString(softwareVersion)},"proxy":${proxy?.let(::jsonString) ?: "null"},"batch":$batch,"verbose":$verbose,"log_handler":${jsonString(logHandler.value)}}"""

private fun jsonString(value: String): String = CliJson.str(value)
