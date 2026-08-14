package org.ooni.probe.core

import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementWithUrl
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.ResultWithNetworkAndAggregates

/**
 * CLI-facing storage surface exposed by probeCore.
 *
 * It wraps the SQLDelight repositories and the DataStore preference store so that `:cliApp`
 * never touches the database schema, driver, or preference keys directly (keeping the CLI thin
 * and the boundary in [org.ooni.probe.data.repositories]).
 *
 * This covers the backend-free commands (`list`, `show`, `rm`, `onboard`, `reset`). The
 * engine-backed commands (`run`, `upload`, `geoip`, `autorun`) require promoting the engine /
 * orchestration layer from `:composeApp` into `:probeCore` (the deferred core-split milestone).
 */
interface CliStorageGateway {
    suspend fun listResults(): List<ResultWithNetworkAndAggregates>

    suspend fun resultExists(resultId: ResultModel.Id): Boolean

    suspend fun listMeasurements(resultId: ResultModel.Id): List<MeasurementWithUrl>

    suspend fun getMeasurement(measurementId: MeasurementModel.Id): MeasurementWithUrl?

    /** Deletes the result and its measurements. Returns false if the result did not exist. */
    suspend fun deleteResult(resultId: ResultModel.Id): Boolean

    suspend fun deleteAllResults()

    /** True once onboarding consent has been recorded (SettingsKey.FIRST_RUN == false). */
    suspend fun isOnboardingComplete(): Boolean

    suspend fun completeOnboarding()

    /** Releases the database driver and preference-store resources. */
    fun close()
}

/**
 * Paths the CLI storage gateway needs. Both are derived from the resolved OONI home so tests can
 * point them at a temp directory and never touch real user data.
 */
data class CliStorageConfig(
    /** Directory that holds `probe.db` (the CLI resolves this to `<ooniHome>/data`). */
    val databaseDir: String,
    /** Absolute path of the DataStore preferences file. */
    val preferencesFile: String,
)
