package org.ooni.probe.cli

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.core.CliCoreGateway
import org.ooni.probe.core.CliCoreGatewayDependencies
import org.ooni.probe.core.CliResetGateway
import org.ooni.probe.core.CliStorageGateway
import org.ooni.probe.core.CliUploadGateway
import org.ooni.probe.core.CliUploadProgress
import org.ooni.probe.core.DescriptorAssetProvider
import org.ooni.probe.core.DescriptorAssetSet
import org.ooni.probe.core.NativeRuntimeBootstrap
import org.ooni.probe.core.NativeRuntimeBootstrapResult
import org.ooni.probe.data.models.MeasurementCounts
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementWithUrl
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.ResultWithNetworkAndAggregates
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CliStorageCommandsTest {
    private val defaultRuntime = CliRuntime(
        paths = CliPathLayout.create(ooniHome = Path.of("/tmp/ooni-cli-test-home"), tempDir = Path.of("/tmp")),
    )

    // ---- list ----

    @Test
    fun listResultsAsJson() {
        val gateway = FakeCliStorageGateway(results = mutableListOf(result(1, "web_connectivity", done = 3, anomaly = 1)))
        val result = runCommand("list", "--json", storage = gateway)

        assertEquals(0, result.code)
        val array = Json.parseToJsonElement(result.stdout.single()).jsonArray
        assertEquals(1, array.size)
        val obj = array[0].jsonObject
        assertEquals(1L, obj["id"]!!.jsonPrimitive.long)
        assertEquals("web_connectivity", obj["name"]!!.jsonPrimitive.content)
        assertEquals(3L, obj["measurements"]!!.jsonPrimitive.long)
        assertEquals(1L, obj["anomalies"]!!.jsonPrimitive.long)
        assertTrue(gateway.closed)
    }

    @Test
    fun listResultsHumanEmpty() {
        val result = runCommand("list")
        assertEquals(0, result.code)
        assertEquals(listOf("No results."), result.stdout)
    }

    @Test
    fun listMeasurementsForResultAsJson() {
        val gateway = FakeCliStorageGateway(
            results = mutableListOf(result(1, "web_connectivity")),
            measurements = mutableMapOf(1L to listOf(measurement(10, uploaded = true))),
        )
        val result = runCommand("list", "1", "--json", storage = gateway)

        assertEquals(0, result.code)
        val array = Json.parseToJsonElement(result.stdout.single()).jsonArray
        assertEquals(1, array.size)
        assertEquals(10L, array[0].jsonObject["id"]!!.jsonPrimitive.long)
        assertEquals(true, array[0].jsonObject["is_uploaded"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun listMeasurementsForMissingResultFails() {
        val gateway = FakeCliStorageGateway(results = mutableListOf(result(1, "web_connectivity")))
        val result = runCommand("list", "999", storage = gateway)

        assertEquals(RUNTIME_ERROR_EXIT_CODE, result.code)
        assertTrue(result.stderr.any { it.contains("Result not found: 999") })
    }

    @Test
    fun listRejectsNonPositiveId() {
        assertEquals(USAGE_ERROR_EXIT_CODE, runCommand("list", "0").code)
    }

    @Test
    fun listRejectsNonNumericId() {
        assertEquals(USAGE_ERROR_EXIT_CODE, runCommand("list", "abc").code)
    }

    // ---- show ----

    @Test
    fun showMeasurementEmbedsTestKeys() {
        val gateway = FakeCliStorageGateway(
            measurementsById = mutableMapOf(10L to measurement(10, resultId = 1, testKeys = """{"blocking":false}""")),
        )
        val result = runCommand("show", "10", storage = gateway)

        assertEquals(0, result.code)
        val obj = Json.parseToJsonElement(result.stdout.single()).jsonObject
        assertEquals(10L, obj["id"]!!.jsonPrimitive.long)
        assertEquals(TestType.WebConnectivity.name, obj["test_name"]!!.jsonPrimitive.content)
        assertEquals(1L, obj["result_id"]!!.jsonPrimitive.long)
        assertEquals(false, obj["test_keys"]!!.jsonObject["blocking"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun showMissingMeasurementFails() {
        val result = runCommand("show", "999", storage = FakeCliStorageGateway())
        assertEquals(RUNTIME_ERROR_EXIT_CODE, result.code)
        assertTrue(result.stderr.any { it.contains("Measurement not found: 999") })
    }

    @Test
    fun showRequiresArgument() {
        assertEquals(USAGE_ERROR_EXIT_CODE, runCommand("show").code)
    }

    @Test
    fun showRejectsNonNumericId() {
        assertEquals(USAGE_ERROR_EXIT_CODE, runCommand("show", "abc").code)
    }

    // ---- rm ----

    @Test
    fun removeResultWithYes() {
        val gateway = FakeCliStorageGateway(results = mutableListOf(result(1, "web_connectivity")))
        val result = runCommand("rm", "1", "--yes", storage = gateway)

        assertEquals(0, result.code)
        assertEquals(listOf(1L), gateway.deletedResultIds)
    }

    @Test
    fun removeAllWithYes() {
        val gateway = FakeCliStorageGateway(results = mutableListOf(result(1, "a"), result(2, "b")))
        val result = runCommand("rm", "--all", "--yes", storage = gateway)

        assertEquals(0, result.code)
        assertTrue(gateway.deleteAllCalled)
    }

    @Test
    fun removeIdAndAllTogetherFails() {
        assertEquals(USAGE_ERROR_EXIT_CODE, runCommand("rm", "1", "--all").code)
    }

    @Test
    fun removeWithoutTargetFails() {
        assertEquals(USAGE_ERROR_EXIT_CODE, runCommand("rm").code)
    }

    @Test
    fun removeInBatchWithoutYesFails() {
        val gateway = FakeCliStorageGateway(results = mutableListOf(result(1, "a")))
        val result = runCommand("rm", "1", "--batch", storage = gateway)
        assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
        assertTrue(gateway.deletedResultIds.isEmpty())
    }

    @Test
    fun removeInteractiveConfirmDeletes() {
        val gateway = FakeCliStorageGateway(results = mutableListOf(result(1, "a")))
        val result = runCommand("rm", "1", storage = gateway, input = { "yes" })
        assertEquals(0, result.code)
        assertEquals(listOf(1L), gateway.deletedResultIds)
    }

    @Test
    fun removeInteractiveDeclineAborts() {
        val gateway = FakeCliStorageGateway(results = mutableListOf(result(1, "a")))
        val result = runCommand("rm", "1", storage = gateway, input = { "no" })
        assertEquals(0, result.code)
        assertTrue(gateway.deletedResultIds.isEmpty())
        assertTrue(result.stdout.any { it == "Aborted." })
    }

    @Test
    fun removeMissingResultFails() {
        val result = runCommand("rm", "999", "--yes", storage = FakeCliStorageGateway())
        assertEquals(RUNTIME_ERROR_EXIT_CODE, result.code)
    }

    // ---- onboard ----

    @Test
    fun onboardWithYesCompletes() {
        val gateway = FakeCliStorageGateway()
        val result = runCommand("onboard", "--yes", storage = gateway)
        assertEquals(0, result.code)
        assertTrue(gateway.onboarded)
    }

    @Test
    fun onboardInBatchWithoutYesFails() {
        val gateway = FakeCliStorageGateway()
        val result = runCommand("onboard", "--batch", storage = gateway)
        assertEquals(USAGE_ERROR_EXIT_CODE, result.code)
        assertFalse(gateway.onboarded)
    }

    @Test
    fun onboardWhenAlreadyComplete() {
        val gateway = FakeCliStorageGateway(onboarded = true)
        val result = runCommand("onboard", storage = gateway)
        assertEquals(0, result.code)
        assertTrue(result.stdout.any { it.contains("already complete") })
    }

    @Test
    fun onboardInteractiveConfirm() {
        val gateway = FakeCliStorageGateway()
        val result = runCommand("onboard", storage = gateway, input = { "yes" })
        assertEquals(0, result.code)
        assertTrue(gateway.onboarded)
    }

    // ---- reset ----

    @Test
    fun resetRequiresForce() {
        assertEquals(USAGE_ERROR_EXIT_CODE, runCommand("reset").code)
    }

    @Test
    fun resetForceClearsSecureStorageDeletesOwnedPathsAndPreservesExternalConfig() {
        val home = Files.createTempDirectory("cli-reset-test")
        try {
            val dataDir = Files.createDirectories(home.resolve("data"))
            Files.writeString(dataDir.resolve("probe.db"), "db")
            val external = Files.createDirectories(home.parent.resolve("external-${home.fileName}"))
            val externalConfig = external.resolve("config.json")
            Files.writeString(externalConfig, "{}")

            val runtime = CliRuntime(
                paths = CliPathLayout.create(ooniHome = home, tempDir = home.resolve("tmp")),
            ).withInvocation(CliInvocationOptions(configFile = externalConfig))

            // Shared ordering log proves DB/core resources close, then secure storage clears, before delete.
            val order = mutableListOf<String>()
            val storage = FakeCliStorageGateway(order = order)
            val reset = FakeCliResetGateway(order = order)
            val result = runCommand("reset", "--force", runtime = runtime, storage = storage, reset = reset)

            assertEquals(0, result.code)
            assertTrue(reset.cleared, "secure storage should be cleared")
            assertTrue(reset.closed, "reset gateway should be closed")
            assertEquals(listOf("db-closed", "secure-cleared"), order)
            assertFalse(Files.exists(dataDir), "data dir should be removed")
            assertTrue(Files.exists(externalConfig), "external config outside home must be preserved")
            assertTrue(result.stdout.any { it == "Secure storage cleared." })
            assertFalse(
                result.stdout.any { it.contains("deferred") },
                "the deferred-secure-storage note must be gone",
            )
        } finally {
            home.toFile().deleteRecursively()
            home.parent.resolve("external-${home.fileName}").toFile().deleteRecursively()
        }
    }

    @Test
    fun resetForceDeletesInHomeConfig() {
        val home = Files.createTempDirectory("cli-reset-inhome")
        try {
            val configFile = home.resolve("config.json")
            Files.writeString(configFile, "{}")
            val runtime = CliRuntime(
                paths = CliPathLayout.create(ooniHome = home, tempDir = home.resolve("tmp")),
            )

            val result = runCommand("reset", "--force", runtime = runtime)

            assertEquals(0, result.code)
            assertFalse(Files.exists(configFile), "in-home config file should be deleted by reset")
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun resetAbortsFilesystemDeletionWhenSecureStorageClearFails() {
        val home = Files.createTempDirectory("cli-reset-fail")
        try {
            val dataDir = Files.createDirectories(home.resolve("data"))
            Files.writeString(dataDir.resolve("probe.db"), "db")
            val runtime = CliRuntime(
                paths = CliPathLayout.create(ooniHome = home, tempDir = home.resolve("tmp")),
            )

            val storage = FakeCliStorageGateway()
            val reset = FakeCliResetGateway(failWith = IllegalStateException("keychain locked"))
            val result = runCommand("reset", "--force", runtime = runtime, storage = storage, reset = reset)

            assertEquals(RUNTIME_ERROR_EXIT_CODE, result.code)
            assertTrue(Files.exists(dataDir), "filesystem deletion must be aborted after a failed clear")
            assertTrue(storage.closed, "DB/core resources should be closed before the abort")
            assertTrue(reset.closed, "reset gateway should be closed even on failure")
            assertFalse(reset.cleared)
            assertTrue(result.stderr.any { it.contains("Failed to clear secure storage") })
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun resetRefusesUnsafeHome() {
        val userHome = Path.of(System.getProperty("user.home"))
        val runtime = CliRuntime(
            paths = CliPathLayout.create(ooniHome = userHome, tempDir = Path.of("/tmp")),
        )
        val reset = FakeCliResetGateway()
        val result = runCommand("reset", "--force", runtime = runtime, reset = reset)

        assertEquals(RUNTIME_ERROR_EXIT_CODE, result.code)
        assertFalse(reset.cleared, "unsafe home must be refused before any secure-storage clearing")
        assertTrue(result.stderr.any { it.contains("Refusing to reset unsafe OONI home") })
    }

    // ---- internal descriptor-load ----

    @Test
    fun internalDescriptorLoadEmitsCounts() {
        val result = runCommand("internal", "descriptor-load", core = FakeCoreGateway())

        assertEquals(0, result.code)
        val obj = Json.parseToJsonElement(result.stdout.single()).jsonObject
        assertEquals("ooni", obj["default"]!!.jsonPrimitive.content)
        assertTrue(obj.containsKey("native_libraries"))
        val descriptors = obj["descriptors"]!!.jsonObject
        assertEquals(0L, descriptors["ooni"]!!.jsonPrimitive.long)
    }

    // ---- upload ----

    @Test
    fun uploadEmptyReportsNothing() {
        val gateway = FakeCliUploadGateway()
        val result = runCommand("upload", upload = gateway)
        assertEquals(0, result.code)
        assertEquals(listOf("No measurements to upload."), result.stdout)
        assertEquals(listOf<MeasurementsFilter>(MeasurementsFilter.All), gateway.filters)
        assertTrue(gateway.closed)
    }

    @Test
    fun uploadAllAsJson() {
        val gateway = FakeCliUploadGateway(listOf(CliUploadProgress(2, 1, 3, finished = true)))
        val result = runCommand("upload", "all", "--json", upload = gateway)
        assertEquals(0, result.code)
        val obj = Json.parseToJsonElement(result.stdout.single()).jsonObject
        assertEquals(2L, obj["uploaded"]!!.jsonPrimitive.long)
        assertEquals(1L, obj["failed"]!!.jsonPrimitive.long)
        assertEquals(3L, obj["total"]!!.jsonPrimitive.long)
    }

    @Test
    fun uploadResultFilter() {
        val gateway = FakeCliUploadGateway()
        runCommand("upload", "result", "5", upload = gateway)
        assertEquals(listOf<MeasurementsFilter>(MeasurementsFilter.Result(ResultModel.Id(5))), gateway.filters)
    }

    @Test
    fun uploadMeasurementFilter() {
        val gateway = FakeCliUploadGateway()
        runCommand("upload", "measurement", "7", upload = gateway)
        assertEquals(listOf<MeasurementsFilter>(MeasurementsFilter.Measurement(MeasurementModel.Id(7))), gateway.filters)
    }

    @Test
    fun uploadRejectsUnknownTarget() {
        assertEquals(USAGE_ERROR_EXIT_CODE, runCommand("upload", "bogus").code)
    }

    @Test
    fun uploadResultRequiresId() {
        assertEquals(USAGE_ERROR_EXIT_CODE, runCommand("upload", "result").code)
    }

    @Test
    fun uploadRejectsNonNumericId() {
        assertEquals(USAGE_ERROR_EXIT_CODE, runCommand("upload", "result", "abc").code)
    }

    @Test
    fun uploadAllRejectsExtraId() {
        assertEquals(USAGE_ERROR_EXIT_CODE, runCommand("upload", "all", "1").code)
    }

    // ---- helpers ----

    private fun runCommand(
        vararg args: String,
        runtime: CliRuntime = defaultRuntime,
        storage: CliStorageGateway = FakeCliStorageGateway(),
        core: CliCoreGateway = FakeCoreGateway(),
        upload: CliUploadGateway = FakeCliUploadGateway(),
        reset: CliResetGateway = FakeCliResetGateway(),
        input: () -> String? = { null },
    ): CommandResult {
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()
        val code = OoniprobeCli(
            runtime = runtime,
            coreGatewayFactory = { core },
            storageGatewayFactory = { storage },
            uploadGatewayFactory = { upload },
            resetGatewayFactory = { reset },
            input = input,
        ).run(args.toList().toTypedArray(), stdout::add, stderr::add)
        return CommandResult(code, stdout, stderr)
    }

    private fun result(
        id: Long,
        name: String,
        done: Long = 0,
        anomaly: Long = 0,
        failed: Long = 0,
        uploaded: Boolean = true,
    ) = ResultWithNetworkAndAggregates(
        result = ResultModel(
            id = ResultModel.Id(id),
            taskOrigin = TaskOrigin.OoniRun,
            descriptorName = name,
            descriptorKey = null,
            runId = null,
            isDone = true,
        ),
        network = NetworkModel(name = "Example Net", asn = "AS1234", countryCode = "US", networkType = null),
        measurementCounts = MeasurementCounts(done = done, failed = failed, anomaly = anomaly),
        allMeasurementsUploaded = uploaded,
        anyMeasurementUploadFailed = false,
    )

    private fun measurement(
        id: Long,
        resultId: Long = 1,
        uploaded: Boolean = false,
        anomaly: Boolean = false,
        failed: Boolean = false,
        testKeys: String? = null,
    ) = MeasurementWithUrl(
        measurement = MeasurementModel(
            id = MeasurementModel.Id(id),
            test = TestType.WebConnectivity,
            reportId = null,
            urlId = null,
            resultId = ResultModel.Id(resultId),
            isUploaded = uploaded,
            isAnomaly = anomaly,
            isFailed = failed,
            testKeys = testKeys,
        ),
        url = null,
    )

    private data class CommandResult(
        val code: Int,
        val stdout: List<String>,
        val stderr: List<String>,
    )

    private class FakeCliStorageGateway(
        val results: MutableList<ResultWithNetworkAndAggregates> = mutableListOf(),
        val measurements: MutableMap<Long, List<MeasurementWithUrl>> = mutableMapOf(),
        val measurementsById: MutableMap<Long, MeasurementWithUrl> = mutableMapOf(),
        var onboarded: Boolean = false,
        private val order: MutableList<String>? = null,
    ) : CliStorageGateway {
        val deletedResultIds = mutableListOf<Long>()
        var deleteAllCalled = false
        var closed = false

        override suspend fun listResults() = results.toList()

        override suspend fun resultExists(resultId: ResultModel.Id) = results.any { it.result.id == resultId }

        override suspend fun listMeasurements(resultId: ResultModel.Id) = measurements[resultId.value] ?: emptyList()

        override suspend fun getMeasurement(measurementId: MeasurementModel.Id) = measurementsById[measurementId.value]

        override suspend fun deleteResult(resultId: ResultModel.Id): Boolean {
            if (results.none { it.result.id == resultId }) return false
            deletedResultIds.add(resultId.value)
            results.removeAll { it.result.id == resultId }
            return true
        }

        override suspend fun deleteAllResults() {
            deleteAllCalled = true
            results.clear()
        }

        override suspend fun isOnboardingComplete() = onboarded

        override suspend fun completeOnboarding() {
            onboarded = true
        }

        override fun close() {
            order?.add("db-closed")
            closed = true
        }
    }

    private class FakeCliResetGateway(
        private val failWith: Exception? = null,
        private val order: MutableList<String>? = null,
    ) : CliResetGateway {
        var cleared = false
        var closed = false

        override suspend fun clearSecureStorage() {
            failWith?.let { throw it }
            order?.add("secure-cleared")
            cleared = true
        }

        override fun close() {
            closed = true
        }
    }

    private class FakeCoreGateway : CliCoreGatewayDependencies {
        override val descriptorAssets = DescriptorAssetProvider { "[]" }
        override val defaultDescriptorAssetSet = DescriptorAssetSet.Ooni
        override val nativeRuntimeBootstrap = object : NativeRuntimeBootstrap {
            override fun configure() = NativeRuntimeBootstrapResult(emptyList())
        }
    }

    private class FakeCliUploadGateway(
        private val progress: List<CliUploadProgress> = emptyList(),
    ) : CliUploadGateway {
        val filters = mutableListOf<MeasurementsFilter>()
        var cancelled = false
        var closed = false

        override fun uploadMissing(filter: MeasurementsFilter): Flow<CliUploadProgress> {
            filters.add(filter)
            return progress.asFlow()
        }

        override fun cancel() {
            cancelled = true
        }

        override fun close() {
            closed = true
        }
    }
}
