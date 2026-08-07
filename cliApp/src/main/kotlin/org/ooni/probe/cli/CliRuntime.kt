package org.ooni.probe.cli

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute
import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.TaskLogLevel
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TaskSettings

object CliConfigContract {
    val supportedDefaultKeys: Set<String> = emptySet()
}

enum class CliLogHandler(
    val value: String,
) {
    Cli("cli"),
    Batch("batch"),
    Syslog("syslog"),
    ;

    companion object {
        fun fromValue(value: String): CliLogHandler = entries.first { it.value == value }
    }
}

data class CliInvocationOptions(
    val configFile: Path? = null,
    val softwareName: String? = null,
    val softwareVersion: String? = null,
    val proxy: String? = null,
    val batch: Boolean? = null,
    val verbose: Boolean? = null,
    val logHandler: CliLogHandler? = null,
    val jsonOutput: Boolean? = null,
) {
    fun merge(other: CliInvocationOptions): CliInvocationOptions =
        CliInvocationOptions(
            configFile = mergeValue("--config", configFile, other.configFile),
            softwareName = mergeValue("--software-name", softwareName, other.softwareName),
            softwareVersion = mergeValue("--software-version", softwareVersion, other.softwareVersion),
            proxy = mergeValue("--proxy", proxy, other.proxy),
            batch = mergeValue("--batch", batch, other.batch),
            verbose = mergeValue("--verbose", verbose, other.verbose),
            logHandler = mergeValue("--log-handler", logHandler, other.logHandler),
            jsonOutput = mergeValue("--json", jsonOutput, other.jsonOutput),
        )

    private fun <T> mergeValue(
        name: String,
        first: T?,
        second: T?,
    ): T? =
        when {
            first == null -> second
            second == null || first == second -> first
            else -> throw CliRuntimeValidationException("Conflicting values for $name")
        }
}

data class CliPathLayout(
    val ooniHome: Path,
    val configFile: Path,
    val tempDir: Path,
) {
    val dataDir: Path = ooniHome.resolve("data")
    val databaseFile: Path = dataDir.resolve("probe.db")
    val preferenceDataStoreFile: Path = dataDir.resolve("probe.preferences_pb")
    val stateDir: Path = dataDir.resolve("state")
    val cacheDir: Path = ooniHome.resolve("cache")
    val logsDir: Path = ooniHome.resolve("logs")
    val tunnelDir: Path = ooniHome.resolve("tunnel")
    val assetsDir: Path = ooniHome.resolve("assets")
    val geoIpDb: Path = assetsDir.resolve("geoip.mmdb")
    val secureStorageDir: Path = ooniHome.resolve("secure-storage")

    fun taskSettingsPaths() = CliTaskSettingsPaths(
        stateDir = stateDir,
        tempDir = tempDir,
        tunnelDir = tunnelDir,
        assetsDir = assetsDir,
        geoIpDb = geoIpDb,
    )

    fun resetDeletionSet(): Set<Path> = buildSet {
        add(dataDir)
        add(cacheDir)
        add(logsDir)
        add(tunnelDir)
        add(assetsDir)
        add(secureStorageDir)
        if (tempDir.isWithin(ooniHome)) add(tempDir)
        if (configFile.isWithin(ooniHome)) add(configFile)
    }

    fun withConfigFile(configFile: Path): CliPathLayout = copy(configFile = canonical(configFile))

    private fun Path.isWithin(parent: Path): Boolean = startsWith(parent)

    companion object {
        fun create(
            ooniHome: Path,
            configFile: Path = ooniHome.resolve("config.json"),
            tempDir: Path,
        ): CliPathLayout =
            CliPathLayout(
                ooniHome = canonical(ooniHome),
                configFile = canonical(configFile),
                tempDir = canonical(tempDir),
            )

        private fun canonical(path: Path): Path = path.absolute().normalize()
    }
}

data class CliTaskSettingsPaths(
    val stateDir: Path,
    val tempDir: Path,
    val tunnelDir: Path,
    val assetsDir: Path,
    val geoIpDb: Path,
)

data class CliTaskSettingsMapping(
    val paths: CliTaskSettingsPaths,
    val logLevel: TaskLogLevel,
    val options: TaskSettings.Options,
    val annotations: TaskSettings.Annotations,
    val proxy: String?,
)

data class CliRuntime(
    val paths: CliPathLayout,
    val softwareName: String = "ooniprobe",
    val softwareVersion: String = CliBuildConfig.VERSION_NAME,
    val proxy: String? = null,
    val batch: Boolean = false,
    val verbose: Boolean = false,
    val logHandler: CliLogHandler = CliLogHandler.Cli,
    val jsonOutput: Boolean = false,
) {
    val ooniHome: String get() = paths.ooniHome.toString()
    val tempDir: String get() = paths.tempDir.toString()

    fun taskSettingsMapping() = CliTaskSettingsMapping(
        paths = paths.taskSettingsPaths(),
        logLevel = if (verbose) TaskLogLevel.Debug else TaskLogLevel.Info,
        options = TaskSettings.Options(
            noCollector = true,
            softwareName = softwareName,
            softwareVersion = softwareVersion,
            maxRuntime = MAX_RUNTIME_DISABLED,
        ),
        annotations = TaskSettings.Annotations(
            networkType = NetworkType.Unknown("unknown"),
            flavor = softwareName,
            origin = TaskOrigin.OoniRun,
            osVersion = System.getProperty("os.version"),
        ),
        proxy = proxy,
    )

    fun withInvocation(options: CliInvocationOptions): CliRuntime {
        val resolved = copy(
            paths = options.configFile?.let(paths::withConfigFile) ?: paths,
            softwareName = options.softwareName ?: softwareName,
            softwareVersion = options.softwareVersion ?: softwareVersion,
            proxy = options.proxy ?: proxy,
            batch = options.batch ?: batch,
            verbose = options.verbose ?: verbose,
            logHandler = options.logHandler ?: if (options.batch == true) CliLogHandler.Batch else logHandler,
            jsonOutput = options.jsonOutput ?: jsonOutput,
        )
        resolved.validate(options.logHandler != null)
        return resolved
    }

    private fun validate(logHandlerWasExplicit: Boolean) {
        if (Files.isDirectory(paths.configFile)) {
            throw CliRuntimeValidationException("--config must be a file path: ${paths.configFile}")
        }
        val proxyValue = proxy
        if (proxyValue != null) {
            val uri = runCatching { URI(proxyValue) }.getOrElse {
                throw CliRuntimeValidationException("Invalid proxy URL: $proxy")
            }
            if (uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) {
                throw CliRuntimeValidationException("Invalid proxy URL: $proxy")
            }
        }
        if (batch && logHandlerWasExplicit) {
            throw CliRuntimeValidationException("--batch cannot be combined with an explicit --log-handler")
        }
        if (logHandler == CliLogHandler.Syslog) {
            throw CliRuntimeValidationException("Log handler syslog is unsupported on this platform")
        }
    }

    companion object {
        private const val MAX_RUNTIME_DISABLED = -1

        fun default(): CliRuntime = fromEnvironment()

        fun fromEnvironment(
            environment: Map<String, String> = System.getenv(),
            userHome: Path = Path.of(System.getProperty("user.home")),
            tempDir: Path = Path.of(System.getProperty("java.io.tmpdir")),
        ): CliRuntime {
            val ooniHome = environment["OONI_HOME"]?.takeIf(String::isNotBlank)?.let(Path::of)
                ?: userHome.resolve(".ooniprobe")
            return CliRuntime(paths = CliPathLayout.create(ooniHome = ooniHome, tempDir = tempDir))
        }
    }
}

class CliRuntimeValidationException(
    message: String,
) : IllegalArgumentException(message)
