package org.ooni.probe.cli

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.core.CliRunGateway
import org.ooni.probe.core.CliRunOptions
import org.ooni.probe.core.CliRunProgress
import org.ooni.probe.core.CliStorageGateway
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementWithUrl
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.OoniTest
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.ResultWithNetworkAndAggregates
import org.ooni.probe.data.models.RunSpecification
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RunCommandTest {
    private val defaultRuntime = CliRuntime(
        paths = CliPathLayout.create(ooniHome = Path.of("/tmp/ooni-cli-run-home"), tempDir = Path.of("/tmp")),
    )

    @Test
    fun bareRunEqualsRunAll() {
        val bare = runCli("run")
        val all = runCli("run", "all")

        assertEquals(0, bare.code)
        assertEquals(0, all.code)
        assertEquals(all.spec(), bare.spec())
    }

    @Test
    fun runAllCoversExactlySixGroupsMergedPerDescriptor() {
        val result = runCli("run", "all")
        val spec = result.spec()

        // websites, performance, middlebox, im, circumvention, experimental -> performance+middlebox
        // share the Performance descriptor id, so 6 groups collapse to 5 Test entries.
        assertEquals(
            setOf(
                OoniTest.Websites.id,
                OoniTest.Performance.id,
                OoniTest.InstantMessaging.id,
                OoniTest.Circumvention.id,
                OoniTest.Experimental.id,
            ),
            spec.tests.map { it.descriptorId.value }.toSet(),
        )
        // Performance Test carries both performance and middlebox subsets, deduped, no duplicates.
        val performance = spec.testFor(OoniTest.Performance)
        assertEquals(4, performance.netTests.size)
        assertEquals(
            setOf("ndt", "dash", "http_header_field_manipulation", "http_invalid_request_line"),
            performance.testNames(),
        )
    }

    @Test
    fun websitesGroupMapsToWebConnectivity() {
        val spec = runCli("run", "websites").spec()
        val websites = spec.testFor(OoniTest.Websites)
        assertEquals(listOf(TestType.WebConnectivity), websites.netTests.map { it.test })
    }

    @Test
    fun imGroupMapsToFourApps() {
        val spec = runCli("run", "im").spec()
        assertEquals(
            setOf("whatsapp", "telegram", "facebook_messenger", "signal"),
            spec.testFor(OoniTest.InstantMessaging).testNames(),
        )
    }

    @Test
    fun circumventionGroupMapsToPsiphonAndTor() {
        val spec = runCli("run", "circumvention").spec()
        assertEquals(setOf("psiphon", "tor"), spec.testFor(OoniTest.Circumvention).testNames())
    }

    @Test
    fun performanceGroupMapsToNdtAndDashOnly() {
        val spec = runCli("run", "performance").spec()
        assertEquals(setOf("ndt", "dash"), spec.testFor(OoniTest.Performance).testNames())
    }

    @Test
    fun middleboxGroupMapsToHttpTestsOnly() {
        val spec = runCli("run", "middlebox").spec()
        assertEquals(
            setOf("http_invalid_request_line", "http_header_field_manipulation"),
            spec.testFor(OoniTest.Performance).testNames(),
        )
    }

    @Test
    fun experimentalGroupMapsToThreePresentNettests() {
        val spec = runCli("run", "experimental").spec()
        assertEquals(
            setOf("stunreachability", "openvpn", "vanilla_tor"),
            spec.testFor(OoniTest.Experimental).testNames(),
        )
    }

    @Test
    fun unattendedExcludesPerformanceKeepingMiddleboxOnly() {
        val spec = runCli("run", "unattended").spec()
        // No `performance` group -> the shared Performance descriptor keeps only the middlebox subset.
        assertEquals(
            setOf("http_header_field_manipulation", "http_invalid_request_line"),
            spec.testFor(OoniTest.Performance).testNames(),
        )
        assertEquals(2, spec.testFor(OoniTest.Performance).netTests.size)
        // unattended membership excludes the `performance` NDT/Dash subset entirely.
        assertEquals(
            setOf(
                OoniTest.Websites.id,
                OoniTest.Performance.id,
                OoniTest.InstantMessaging.id,
                OoniTest.Circumvention.id,
                OoniTest.Experimental.id,
            ),
            spec.tests.map { it.descriptorId.value }.toSet(),
        )
    }

    @Test
    fun taskOriginIsOoniRun() {
        assertEquals(TaskOrigin.OoniRun, runCli("run", "all").spec().taskOrigin)
    }

    @Test
    fun websitesInputsMergeInputThenInputFileInOrder() {
        val file = Files.createTempFile("cli-run-input", ".txt")
        Files.write(file, listOf("https://c.example", "", "https://d.example"))
        try {
            val spec = runCli(
                "run", "websites",
                "--input", "https://a.example",
                "--input", "https://b.example",
                "--input-file", file.toString(),
            ).spec()
            val webConnectivity = spec.testFor(OoniTest.Websites).netTests.single()
            assertEquals(
                listOf("https://a.example", "https://b.example", "https://c.example", "https://d.example"),
                webConnectivity.inputs,
            )
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun websitesWithNoInputsIsValid() {
        val result = runCli("run", "websites")
        assertEquals(0, result.code)
        val webConnectivity = result.spec().testFor(OoniTest.Websites).netTests.single()
        assertEquals(TestType.WebConnectivity, webConnectivity.test)
        assertTrue(webConnectivity.inputs.isNullOrEmpty())
    }

    @Test
    fun noCollectorAndNoCredsReflectedInOptions() {
        val factory = FakeCliRunGatewayFactory()
        runCli("run", "performance", "--no-collector", "--no-creds", run = factory)
        assertEquals(CliRunOptions(noCollector = true, noCreds = true), factory.gateway.options.single())
    }

    @Test
    fun defaultOptionsAreNeitherFlag() {
        val factory = FakeCliRunGatewayFactory()
        runCli("run", "performance", run = factory)
        assertEquals(CliRunOptions(noCollector = false, noCreds = false), factory.gateway.options.single())
    }

    @Test
    fun onboardingCompleteLetsRunProceed() {
        val factory = FakeCliRunGatewayFactory()
        val result = runCli("run", "performance", run = factory, storage = FakeRunStorageGateway(onboarded = true))
        assertEquals(0, result.code)
        assertEquals(1, factory.gateway.specs.size)
        assertTrue(factory.gateway.closed)
    }

    @Test
    fun jsonSummaryIsParseable() {
        val result = runCli("run", "performance", "--json")
        assertEquals(0, result.code)
        val obj = Json.parseToJsonElement(result.stdout.single()).jsonObject
        assertEquals(listOf("performance"), obj["groups"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(2L, obj["tests"]!!.jsonPrimitive.long)
    }

    private fun runCli(
        vararg args: String,
        runtime: CliRuntime = defaultRuntime,
        run: FakeCliRunGatewayFactory = FakeCliRunGatewayFactory(),
        storage: CliStorageGateway = FakeRunStorageGateway(onboarded = true),
    ): RunCliResult = runRunCommand(args = args, runtime = runtime, run = run, storage = storage)
}

class RunCommandFailureTest {
    private val defaultRuntime = CliRuntime(
        paths = CliPathLayout.create(ooniHome = Path.of("/tmp/ooni-cli-run-home"), tempDir = Path.of("/tmp")),
    )

    @Test
    fun invalidUrlLineReportsFileAndLine() {
        val file = Files.createTempFile("cli-run-bad", ".txt")
        Files.write(file, listOf("https://good.example", "not a url"))
        val factory = FakeCliRunGatewayFactory()
        try {
            val result = runCli("run", "websites", "--input-file", file.toString(), run = factory)
            assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
            assertTrue(result.stderr.any { it.contains("$file:2") }, "expected file:line diagnostic, got ${result.stderr}")
            assertRunNeverStarted(factory)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun emptyInputValueFails() {
        val factory = FakeCliRunGatewayFactory()
        val result = runCli("run", "websites", "--input", "", run = factory)
        assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
        assertRunNeverStarted(factory)
    }

    @Test
    fun inputFileWithNoUsableEntriesFails() {
        val file = Files.createTempFile("cli-run-blank", ".txt")
        Files.write(file, listOf("", "   ", ""))
        val factory = FakeCliRunGatewayFactory()
        try {
            val result = runCli("run", "websites", "--input-file", file.toString(), run = factory)
            assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
            assertRunNeverStarted(factory)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun missingInputFileFails() {
        val factory = FakeCliRunGatewayFactory()
        val result = runCli("run", "websites", "--input-file", "/tmp/does-not-exist-cli-run.txt", run = factory)
        assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
        assertTrue(result.stderr.any { it.contains("not found") })
        assertRunNeverStarted(factory)
    }

    @Test
    fun commentLineIsInvalidUrl() {
        val file = Files.createTempFile("cli-run-comment", ".txt")
        Files.write(file, listOf("#comment"))
        val factory = FakeCliRunGatewayFactory()
        try {
            val result = runCli("run", "websites", "--input-file", file.toString(), run = factory)
            assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
            assertTrue(result.stderr.any { it.contains("$file:1") })
            assertRunNeverStarted(factory)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun invalidGroupNameFails() {
        val factory = FakeCliRunGatewayFactory()
        val result = runCli("run", "bogus", run = factory)
        assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
        assertRunNeverStarted(factory)
    }

    @Test
    fun batchRunWithIncompleteOnboardingFailsBeforeRunGateway() {
        val factory = FakeCliRunGatewayFactory()
        val result = runCli(
            "--batch", "run", "websites",
            run = factory,
            storage = FakeRunStorageGateway(onboarded = false),
        )
        assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
        assertTrue(result.stderr.any { it.contains("onboard") })
        // No run gateway is constructed and no run starts before consent.
        assertEquals(0, factory.creations)
        assertRunNeverStarted(factory)
    }

    private fun assertRunNeverStarted(factory: FakeCliRunGatewayFactory) {
        assertTrue(factory.gateway.specs.isEmpty(), "run gateway must not be invoked on pre-run failure")
    }

    private fun runCli(
        vararg args: String,
        runtime: CliRuntime = defaultRuntime,
        run: FakeCliRunGatewayFactory = FakeCliRunGatewayFactory(),
        storage: CliStorageGateway = FakeRunStorageGateway(onboarded = true),
    ): RunCliResult = runRunCommand(args = args, runtime = runtime, run = run, storage = storage)
}

// ---- shared harness -------------------------------------------------------------------------

internal data class RunCliResult(
    val code: Int,
    val stdout: List<String>,
    val stderr: List<String>,
    val factory: FakeCliRunGatewayFactory,
) {
    fun spec(): RunSpecification.Full = factory.gateway.specs.single() as RunSpecification.Full
}

internal fun runRunCommand(
    args: Array<out String>,
    runtime: CliRuntime,
    run: FakeCliRunGatewayFactory,
    storage: CliStorageGateway,
): RunCliResult {
    val stdout = mutableListOf<String>()
    val stderr = mutableListOf<String>()
    val code = OoniprobeCli(
        runtime = runtime,
        storageGatewayFactory = { storage },
        runGatewayFactory = run,
        input = { null },
    ).run(args.toList().toTypedArray(), stdout::add, stderr::add)
    return RunCliResult(code, stdout, stderr, run)
}

internal fun RunSpecification.Full.testFor(ooniTest: OoniTest): RunSpecification.Test =
    tests.firstOrNull { it.descriptorId.value == ooniTest.id }
        ?: error("no Test for descriptor ${ooniTest.id}")

internal fun RunSpecification.Test.testNames(): Set<String> = netTests.map { it.test.name }.toSet()

internal class FakeCliRunGatewayFactory(
    val gateway: FakeCliRunGateway = FakeCliRunGateway(),
) : CliRunGatewayFactory {
    var creations = 0

    override fun create(runtime: CliRuntime): CliRunGateway {
        creations++
        return gateway
    }
}

internal class FakeCliRunGateway : CliRunGateway {
    val specs = mutableListOf<RunSpecification>()
    val options = mutableListOf<CliRunOptions>()
    var cancelled = false
    var closed = false

    override fun run(spec: RunSpecification, options: CliRunOptions): Flow<CliRunProgress> {
        specs.add(spec)
        this.options.add(options)
        return listOf(CliRunProgress(CliRunProgress.Phase.Idle, finished = true)).asFlow()
    }

    override suspend fun descriptors(): List<Descriptor> = BUNDLED_OONI_DESCRIPTORS

    override fun cancel() {
        cancelled = true
    }

    override fun close() {
        closed = true
    }

    private companion object {
        // Mirrors the bundled OONI descriptor set the real gateway loads (ids + netTest ordering
        // from probeCore/src/desktopMain/resources/assets/descriptors/ooni.json).
        val BUNDLED_OONI_DESCRIPTORS = listOf(
            ooniDescriptor(OoniTest.Websites.id, NetTest(TestType.WebConnectivity)),
            ooniDescriptor(
                OoniTest.InstantMessaging.id,
                NetTest(TestType.Whatsapp),
                NetTest(TestType.Telegram),
                NetTest(TestType.FacebookMessenger),
                NetTest(TestType.Signal),
            ),
            ooniDescriptor(OoniTest.Circumvention.id, NetTest(TestType.Psiphon), NetTest(TestType.Tor)),
            ooniDescriptor(
                OoniTest.Performance.id,
                NetTest(TestType.Ndt),
                NetTest(TestType.Dash),
                NetTest(TestType.HttpHeaderFieldManipulation),
                NetTest(TestType.HttpInvalidRequestLine),
            ),
            ooniDescriptor(
                OoniTest.Experimental.id,
                NetTest(TestType.Experimental("stunreachability")),
                NetTest(TestType.Experimental("openvpn")),
                NetTest(TestType.Experimental("vanilla_tor")),
            ),
        )

        fun ooniDescriptor(id: String, vararg netTests: NetTest) =
            Descriptor(
                id = Descriptor.Id(id),
                revision = 1,
                name = id,
                shortDescription = null,
                description = null,
                author = null,
                netTests = netTests.toList(),
                nameIntl = null,
                shortDescriptionIntl = null,
                descriptionIntl = null,
                icon = null,
                color = null,
                animation = null,
                expirationDate = null,
                dateCreated = null,
                dateUpdated = null,
                dateInstalled = null,
                autoUpdate = false,
            )
    }
}

internal class FakeRunStorageGateway(
    private var onboarded: Boolean = true,
) : CliStorageGateway {
    var closed = false

    override suspend fun listResults(): List<ResultWithNetworkAndAggregates> = emptyList()

    override suspend fun resultExists(resultId: ResultModel.Id): Boolean = false

    override suspend fun listMeasurements(resultId: ResultModel.Id): List<MeasurementWithUrl> = emptyList()

    override suspend fun getMeasurement(measurementId: MeasurementModel.Id): MeasurementWithUrl? = null

    override suspend fun deleteResult(resultId: ResultModel.Id): Boolean = false

    override suspend fun deleteAllResults() = Unit

    override suspend fun isOnboardingComplete(): Boolean = onboarded

    override suspend fun completeOnboarding() {
        onboarded = true
    }

    override fun close() {
        closed = true
    }
}
