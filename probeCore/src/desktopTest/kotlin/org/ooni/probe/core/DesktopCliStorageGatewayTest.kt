package org.ooni.probe.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.Database
import org.ooni.probe.data.buildDatabaseDriver
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.repositories.MeasurementRepository
import org.ooni.probe.data.repositories.ResultRepository
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopCliStorageGatewayTest {
    @Test
    fun listShowRemoveAndOnboardingAgainstTempDatabase() =
        runTest {
            val dataDir = Files.createTempDirectory("cli-storage-test")
            val prefsFile = dataDir.resolve("probe.preferences_pb")
            try {
                // Seed a result + measurement via a separate driver, then close it so the gateway
                // reads the committed on-disk database (WAL), exactly as the CLI would.
                val resultId = seed(dataDir.toString())

                val gateway = buildDesktopCliStorageGateway(
                    CliStorageConfig(databaseDir = dataDir.toString(), preferencesFile = prefsFile.toString()),
                )
                try {
                    val results = gateway.listResults()
                    assertEquals(1, results.size)
                    assertEquals(resultId, results.first().result.id)
                    assertEquals("web_connectivity", results.first().result.descriptorName)

                    assertTrue(gateway.resultExists(resultId))
                    assertFalse(gateway.resultExists(ResultModel.Id(999_999)))

                    val measurements = gateway.listMeasurements(resultId)
                    assertEquals(1, measurements.size)
                    assertEquals(TestType.WebConnectivity, measurements.first().measurement.test)
                    val measurementId = measurements.first().measurement.id
                    assertNotNull(measurementId)

                    val detail = gateway.getMeasurement(measurementId)
                    assertNotNull(detail)
                    assertEquals("""{"blocking":false}""", detail.measurement.testKeys)
                    assertNull(gateway.getMeasurement(MeasurementModel.Id(999_999)))

                    // Onboarding: first run until explicitly completed.
                    assertFalse(gateway.isOnboardingComplete())
                    gateway.completeOnboarding()
                    assertTrue(gateway.isOnboardingComplete())

                    // Removal.
                    assertTrue(gateway.deleteResult(resultId))
                    assertFalse(gateway.deleteResult(resultId))
                    assertEquals(0, gateway.listResults().size)
                    assertEquals(0, gateway.listMeasurements(resultId).size)
                } finally {
                    gateway.close()
                }
            } finally {
                dataDir.toFile().deleteRecursively()
            }
        }

    private suspend fun seed(dataDir: String): ResultModel.Id {
        val driver = buildDatabaseDriver(dataDir)
        val database = Database(driver)
        val json = Json { ignoreUnknownKeys = true }
        val results = ResultRepository(database, Dispatchers.IO)
        val measurements = MeasurementRepository(database, json, Dispatchers.IO)
        val resultId = results.createOrUpdate(
            ResultModel(
                taskOrigin = TaskOrigin.OoniRun,
                descriptorName = "web_connectivity",
                descriptorKey = null,
                runId = null,
                isDone = true,
            ),
        )
        measurements.createOrUpdate(
            MeasurementModel(
                test = TestType.WebConnectivity,
                reportId = null,
                urlId = null,
                resultId = resultId,
                testKeys = """{"blocking":false}""",
                isUploaded = true,
                isDone = true,
            ),
        )
        driver.close()
        return resultId
    }
}
