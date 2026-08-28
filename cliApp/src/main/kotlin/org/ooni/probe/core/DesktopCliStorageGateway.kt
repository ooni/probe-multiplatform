package org.ooni.probe.core

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import org.ooni.probe.Database
import org.ooni.probe.data.buildDatabaseDriver
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementWithUrl
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.ResultWithNetworkAndAggregates
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.MeasurementRepository
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.data.repositories.ResultRepository
import java.io.File

/** Builds the production desktop/JVM CLI storage gateway backed by SQLDelight + DataStore. */
fun buildDesktopCliStorageGateway(config: CliStorageConfig): CliStorageGateway = DesktopCliStorageGateway(config)

private class DesktopCliStorageGateway(
    config: CliStorageConfig,
) : CliStorageGateway {
    private val backgroundContext = Dispatchers.IO
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val driver: SqlDriver = buildDatabaseDriver(config.databaseDir)
    private val database = Database(driver)
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val resultRepository = ResultRepository(database, backgroundContext)
    private val measurementRepository = MeasurementRepository(database, json, backgroundContext)
    private val preferenceRepository = PreferenceRepository(JsonFilePreferencesDataStore(File(config.preferencesFile)))

    override suspend fun listResults(): List<ResultWithNetworkAndAggregates> = resultRepository.list().first()

    override suspend fun resultExists(resultId: ResultModel.Id): Boolean = resultRepository.getById(resultId).first() != null

    override suspend fun listMeasurements(resultId: ResultModel.Id): List<MeasurementWithUrl> =
        measurementRepository.listByResultId(resultId).first()

    override suspend fun getMeasurement(measurementId: MeasurementModel.Id): MeasurementWithUrl? =
        runCatching { measurementRepository.getById(measurementId).first() }.getOrNull()

    override suspend fun deleteResult(resultId: ResultModel.Id): Boolean {
        if (!resultExists(resultId)) return false
        val measurementIds = measurementRepository
            .listByResultId(resultId)
            .first()
            .mapNotNull { it.measurement.id }
        measurementRepository.deleteByIds(measurementIds)
        resultRepository.deleteByIds(listOf(resultId))
        return true
    }

    override suspend fun deleteAllResults() = resultRepository.deleteAll()

    override suspend fun isOnboardingComplete(): Boolean = preferenceRepository.getValueByKey(SettingsKey.FIRST_RUN).first() == false

    override suspend fun completeOnboarding() = preferenceRepository.setValueByKey(SettingsKey.FIRST_RUN, false)

    override fun close() {
        runCatching { driver.close() }
        scope.cancel()
    }
}
