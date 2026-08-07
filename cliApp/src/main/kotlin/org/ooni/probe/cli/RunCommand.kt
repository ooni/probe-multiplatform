package org.ooni.probe.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.core.CliEngineConfig
import org.ooni.probe.core.CliRunGateway
import org.ooni.probe.core.CliRunOptions
import org.ooni.probe.core.CliRunProgress
import org.ooni.probe.core.buildDesktopCliRunGateway
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.OoniTest
import org.ooni.probe.data.models.RunSpecification
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

fun interface CliRunGatewayFactory {
    fun create(runtime: CliRuntime): CliRunGateway
}

internal object ProductionCliRunGatewayFactory : CliRunGatewayFactory {
    override fun create(runtime: CliRuntime): CliRunGateway {
        Files.createDirectories(runtime.paths.dataDir)
        return buildDesktopCliRunGateway(
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

// ---- group -> RunSpecification resolver -----------------------------------------------------

/**
 * Maps CLI virtual run groups onto canonical [RunSpecification.Full] specs, selecting the ACTUAL
 * [NetTest] objects from the loaded bundled descriptors (never synthesizing test types).
 *
 * Each group targets a bundled OONI descriptor id and a subset of that descriptor's netTests. Two
 * groups (`performance`, `middlebox`) share the Performance descriptor id, so [resolve] merges
 * groups per descriptor id and de-duplicates netTests, mirroring probe-cli's virtual groups on top
 * of `RunSpecification.Test(descriptorId, netTests)`.
 */
internal object RunGroupResolver {
    data class Group(
        val name: String,
        val descriptorId: String,
        val includes: (TestType) -> Boolean,
    )

    private val WEBSITES = Group("websites", OoniTest.Websites.id) { it == TestType.WebConnectivity }
    private val IM = Group("im", OoniTest.InstantMessaging.id) {
        it == TestType.Whatsapp || it == TestType.Telegram ||
            it == TestType.FacebookMessenger || it == TestType.Signal
    }
    private val CIRCUMVENTION = Group("circumvention", OoniTest.Circumvention.id) {
        it == TestType.Psiphon || it == TestType.Tor
    }
    private val PERFORMANCE = Group("performance", OoniTest.Performance.id) {
        it == TestType.Ndt || it == TestType.Dash
    }
    private val MIDDLEBOX = Group("middlebox", OoniTest.Performance.id) {
        it == TestType.HttpInvalidRequestLine || it == TestType.HttpHeaderFieldManipulation
    }

    // EXPERIMENTAL PARITY BLOCKER: probe-cli's `run experimental` covers six nettests (DNS Check,
    // ECH Check, STUN Reachability, OpenVPN, Tor Snowflake, Vanilla Tor). The bundled OONI assets +
    // Kotlin `TestType` only provide three (stunreachability, openvpn, vanilla_tor); DNS Check, ECH
    // Check, and Tor Snowflake are ABSENT. We select only the present nettests (never inventing
    // TestTypes). This is a data/engine gap, not a CLI gap. See
    // .omo/artifacts/cli-completion-full-parity/parity-blockers.md.
    private val EXPERIMENTAL_NAMES = setOf("stunreachability", "openvpn", "vanilla_tor")
    private val EXPERIMENTAL = Group("experimental", OoniTest.Experimental.id) {
        it is TestType.Experimental && it.name in EXPERIMENTAL_NAMES
    }

    // Bare `run` == `run all`: union of the manual probe-cli groups, excluding the `unattended`
    // scheduling semantics. performance + middlebox share the Performance descriptor id and are
    // merged into one Test by resolve().
    private val ALL = listOf(WEBSITES, PERFORMANCE, MIDDLEBOX, IM, CIRCUMVENTION, EXPERIMENTAL)

    // `run unattended`: probe-cli unattended-OK groups. NO performance (NDT/Dash) -> the Performance
    // descriptor keeps only [middlebox] nettests. CLI parity group eligibility wins here over the
    // generic `TestType.Experimental.isBackgroundRunEnabled` default (experimental is always
    // included for `unattended` regardless of that flag).
    private val UNATTENDED = listOf(WEBSITES, MIDDLEBOX, IM, CIRCUMVENTION, EXPERIMENTAL)

    private val NAMED: Map<String, List<Group>> = mapOf(
        "websites" to listOf(WEBSITES),
        "im" to listOf(IM),
        "circumvention" to listOf(CIRCUMVENTION),
        "performance" to listOf(PERFORMANCE),
        "middlebox" to listOf(MIDDLEBOX),
        "experimental" to listOf(EXPERIMENTAL),
        "all" to ALL,
        "unattended" to UNATTENDED,
    )

    fun groupsFor(name: String): List<Group> =
        NAMED[name] ?: throw CliRuntimeValidationException("Unknown run group: $name")

    /**
     * Builds a [RunSpecification.Full] by selecting each group's netTests from [descriptors] and
     * merging groups that share a descriptor id (deduping netTests by [TestType]). When
     * [websiteInputs] is non-empty they become the WebConnectivity netTest inputs; otherwise the
     * descriptor's inputs are left untouched so the canonical check-in/fallback path fills them.
     */
    fun resolve(
        descriptors: List<Descriptor>,
        groups: List<Group>,
        websiteInputs: List<String> = emptyList(),
    ): RunSpecification.Full {
        val byDescriptor = LinkedHashMap<String, MutableList<NetTest>>()
        for (group in groups) {
            val descriptor = descriptors.firstOrNull { it.id.value == group.descriptorId }
                ?: throw CliRuntimeValidationException(
                    "Missing bundled descriptor ${group.descriptorId} for group ${group.name}",
                )
            val bucket = byDescriptor.getOrPut(group.descriptorId) { mutableListOf() }
            descriptor.netTests
                .filter { group.includes(it.test) }
                .forEach { netTest ->
                    val resolved =
                        if (websiteInputs.isNotEmpty() && netTest.test == TestType.WebConnectivity) {
                            netTest.copy(inputs = websiteInputs)
                        } else {
                            netTest
                        }
                    if (bucket.none { it.test == resolved.test }) bucket.add(resolved)
                }
        }
        val tests = byDescriptor.map { (id, netTests) ->
            RunSpecification.Test(descriptorId = Descriptor.Id(id), netTests = netTests)
        }
        return RunSpecification.Full(tests = tests, taskOrigin = TaskOrigin.OoniRun)
    }
}

// ---- run command ----------------------------------------------------------------------------

internal class RunCommand(
    private val output: CliOutput,
    private val runGatewayFactory: CliRunGatewayFactory,
    private val storageGatewayFactory: CliStorageGatewayFactory,
    private val signals: CliSignals,
) : CliktCommand(name = "run") {
    override val invokeWithoutSubcommand = true
    private val runtimeOptions by CliRuntimeOptionGroup()
    private val runtimeContext by requireObject<CliRuntimeContext>()
    private val noCollector by option("--no-collector").flag()
    private val noCreds by option("--no-creds").flag()

    init {
        subcommands(
            WebsitesRunCommand(output, runGatewayFactory, storageGatewayFactory, signals),
            ImRunCommand(output, runGatewayFactory, storageGatewayFactory, signals),
            PerformanceRunCommand(output, runGatewayFactory, storageGatewayFactory, signals),
            CircumventionRunCommand(output, runGatewayFactory, storageGatewayFactory, signals),
            MiddleboxRunCommand(output, runGatewayFactory, storageGatewayFactory, signals),
            ExperimentalRunCommand(output, runGatewayFactory, storageGatewayFactory, signals),
            UnattendedRunCommand(output, runGatewayFactory, storageGatewayFactory, signals),
            AllRunCommand(output, runGatewayFactory, storageGatewayFactory, signals),
        )
    }

    override fun run() {
        // A subcommand (group) handles its own execution; bare `run` == `run all`.
        if (currentContext.invokedSubcommand != null) return
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        executeRun(
            output = output,
            runGatewayFactory = runGatewayFactory,
            storageGatewayFactory = storageGatewayFactory,
            signals = signals,
            runtime = runtime,
            groups = RunGroupResolver.groupsFor("all"),
            options = CliRunOptions(noCollector = noCollector, noCreds = noCreds),
        )
    }
}

private abstract class RunGroupCommand(
    name: String,
    protected val output: CliOutput,
    protected val runGatewayFactory: CliRunGatewayFactory,
    protected val storageGatewayFactory: CliStorageGatewayFactory,
    protected val signals: CliSignals,
) : CliktCommand(name = name) {
    protected val runtimeOptions by CliRuntimeOptionGroup()
    protected val runtimeContext by requireObject<CliRuntimeContext>()
    private val noCollector by option("--no-collector").flag()
    private val noCreds by option("--no-creds").flag()

    protected abstract val groupName: String

    /** Websites overrides this to supply validated `--input`/`--input-file` URLs. */
    protected open fun websiteInputs(): List<String> = emptyList()

    override fun run() {
        // Validate inputs (websites) BEFORE resolving the runtime or constructing any gateway so
        // invalid inputs abort with no run.
        val inputs = websiteInputs()
        val runtime = runtimeContext.resolve(runtimeOptions.asInvocation())
        executeRun(
            output = output,
            runGatewayFactory = runGatewayFactory,
            storageGatewayFactory = storageGatewayFactory,
            signals = signals,
            runtime = runtime,
            groups = RunGroupResolver.groupsFor(groupName),
            options = CliRunOptions(noCollector = noCollector, noCreds = noCreds),
            websiteInputs = inputs,
        )
    }
}

private class WebsitesRunCommand(
    output: CliOutput,
    runGatewayFactory: CliRunGatewayFactory,
    storageGatewayFactory: CliStorageGatewayFactory,
    signals: CliSignals,
) : RunGroupCommand("websites", output, runGatewayFactory, storageGatewayFactory, signals) {
    override val groupName = "websites"
    private val inputs by option("--input").multiple()
    private val inputFiles by option("--input-file").path(mustExist = false, canBeDir = false).multiple()

    // Deterministic merge order: all `--input` values (command-line order) first, then each
    // `--input-file` in order, expanding non-blank lines in file order. Blank lines are ignored;
    // every other line must be a valid URL (a `#comment` line is an invalid URL, not a comment).
    override fun websiteInputs(): List<String> {
        val merged = mutableListOf<String>()
        inputs.forEach { value ->
            validateUrl(value) { "Invalid URL from --input: \"$value\"" }
            merged.add(value)
        }
        inputFiles.forEach { file ->
            if (!Files.exists(file)) throw CliRuntimeValidationException("Input file not found: $file")
            var usable = 0
            readLines(file).forEachIndexed { index, raw ->
                if (raw.isBlank()) return@forEachIndexed
                val url = raw.trim()
                validateUrl(url) { "$file:${index + 1}: invalid URL: $url" }
                merged.add(url)
                usable++
            }
            if (usable == 0) throw CliRuntimeValidationException("Input file has no usable URL entries: $file")
        }
        return merged
    }

    private fun readLines(file: Path): List<String> =
        runCatching { Files.readAllLines(file) }
            .getOrElse { throw CliRuntimeValidationException("Unable to read input file: $file") }
}

private class ImRunCommand(
    output: CliOutput,
    runGatewayFactory: CliRunGatewayFactory,
    storageGatewayFactory: CliStorageGatewayFactory,
    signals: CliSignals,
) : RunGroupCommand("im", output, runGatewayFactory, storageGatewayFactory, signals) {
    override val groupName = "im"
}

private class PerformanceRunCommand(
    output: CliOutput,
    runGatewayFactory: CliRunGatewayFactory,
    storageGatewayFactory: CliStorageGatewayFactory,
    signals: CliSignals,
) : RunGroupCommand("performance", output, runGatewayFactory, storageGatewayFactory, signals) {
    override val groupName = "performance"
}

private class CircumventionRunCommand(
    output: CliOutput,
    runGatewayFactory: CliRunGatewayFactory,
    storageGatewayFactory: CliStorageGatewayFactory,
    signals: CliSignals,
) : RunGroupCommand("circumvention", output, runGatewayFactory, storageGatewayFactory, signals) {
    override val groupName = "circumvention"
}

private class MiddleboxRunCommand(
    output: CliOutput,
    runGatewayFactory: CliRunGatewayFactory,
    storageGatewayFactory: CliStorageGatewayFactory,
    signals: CliSignals,
) : RunGroupCommand("middlebox", output, runGatewayFactory, storageGatewayFactory, signals) {
    override val groupName = "middlebox"
}

private class ExperimentalRunCommand(
    output: CliOutput,
    runGatewayFactory: CliRunGatewayFactory,
    storageGatewayFactory: CliStorageGatewayFactory,
    signals: CliSignals,
) : RunGroupCommand("experimental", output, runGatewayFactory, storageGatewayFactory, signals) {
    override val groupName = "experimental"
}

private class UnattendedRunCommand(
    output: CliOutput,
    runGatewayFactory: CliRunGatewayFactory,
    storageGatewayFactory: CliStorageGatewayFactory,
    signals: CliSignals,
) : RunGroupCommand("unattended", output, runGatewayFactory, storageGatewayFactory, signals) {
    override val groupName = "unattended"
}

private class AllRunCommand(
    output: CliOutput,
    runGatewayFactory: CliRunGatewayFactory,
    storageGatewayFactory: CliStorageGatewayFactory,
    signals: CliSignals,
) : RunGroupCommand("all", output, runGatewayFactory, storageGatewayFactory, signals) {
    override val groupName = "all"
}

// ---- shared execution -----------------------------------------------------------------------

private fun executeRun(
    output: CliOutput,
    runGatewayFactory: CliRunGatewayFactory,
    storageGatewayFactory: CliStorageGatewayFactory,
    signals: CliSignals,
    runtime: CliRuntime,
    groups: List<RunGroupResolver.Group>,
    options: CliRunOptions,
    websiteInputs: List<String> = emptyList(),
) {
    // Onboarding preflight: no run gateway is constructed and no run/check-in starts before consent.
    requireOnboarding(storageGatewayFactory, runtime)

    val gateway = runGatewayFactory.create(runtime)
    // A SIGINT/SIGTERM cancels the in-flight run through the canonical core path; the run flow then
    // completes gracefully (cleanup preserved) and we surface exit code 130 below.
    signals.setActive { gateway.cancel() }
    try {
        runBlocking {
            val spec = RunGroupResolver.resolve(gateway.descriptors(), groups, websiteInputs)
            var last: CliRunProgress? = null
            val runningTests = LinkedHashSet<String>()
            gateway.run(spec, options).collect { progress ->
                last = progress
                progress.testType?.let { testType ->
                    if (runningTests.add(testType) && !runtime.jsonOutput) {
                        output.stdout("Running $testType")
                    }
                }
            }
            emitSummary(output, runtime, groups, spec, last)
        }
    } finally {
        signals.clearActive()
        gateway.close()
    }
    if (signals.wasSignalled()) throw ProgramResult(SIGINT_EXIT_CODE)
}

private fun requireOnboarding(
    storageGatewayFactory: CliStorageGatewayFactory,
    runtime: CliRuntime,
) {
    val storage = storageGatewayFactory.create(runtime)
    val complete = try {
        runBlocking { storage.isOnboardingComplete() }
    } finally {
        storage.close()
    }
    if (!complete) {
        throw CliRuntimeValidationException(
            "Onboarding is not complete. Run `ooniprobe onboard --yes` before running measurements.",
        )
    }
}

private fun emitSummary(
    output: CliOutput,
    runtime: CliRuntime,
    groups: List<RunGroupResolver.Group>,
    spec: RunSpecification.Full,
    last: CliRunProgress?,
) {
    val groupNames = groups.map { it.name }.distinct()
    val testCount = spec.tests.sumOf { it.netTests.size }
    val uploaded = last?.uploaded ?: 0
    val failed = last?.failedToUpload ?: 0
    val total = last?.total ?: 0
    if (runtime.jsonOutput) {
        output.stdout(
            CliJson.obj(
                "groups" to CliJson.arr(groupNames.map { CliJson.str(it) }),
                "tests" to CliJson.num(testCount.toLong()),
                "uploaded" to CliJson.num(uploaded.toLong()),
                "failed" to CliJson.num(failed.toLong()),
                "total" to CliJson.num(total.toLong()),
            ),
        )
    } else {
        output.stdout("Ran ${groupNames.joinToString(", ")} ($testCount test(s)).")
        if (total > 0) {
            output.stdout("Uploaded $uploaded/$total ($failed failed).")
        }
    }
}

private fun validateUrl(value: String, message: () -> String) {
    val uri = runCatching { URI(value) }.getOrNull()
    if (uri == null || uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) {
        throw CliRuntimeValidationException(message())
    }
}
