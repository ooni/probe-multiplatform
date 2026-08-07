package org.ooni.probe.core

import kotlinx.coroutines.flow.Flow
import org.ooni.probe.data.models.MeasurementsFilter

/**
 * CLI-facing upload surface: submits measurements that were stored but not yet uploaded.
 *
 * The first-cut implementation uploads anonymously through the engine's submit path
 * (`Engine.submitMeasurement`); user-credential submission (passport) is a follow-up. It does not
 * start new measurements — only existing not-uploaded rows are submitted.
 */
interface CliUploadGateway {
    fun uploadMissing(filter: MeasurementsFilter): Flow<CliUploadProgress>

    /**
     * Cancels an in-flight [uploadMissing] collection so the flow completes gracefully. Cleanup
     * (driver close) still happens via [close]. Safe to call when no upload is active.
     */
    fun cancel()

    fun close()
}

data class CliUploadProgress(
    val uploaded: Int,
    val failedToUpload: Int,
    val total: Int,
    val finished: Boolean,
)

/**
 * Engine-name suffix the CLI reports to the OONI backend, producing `ooniprobe-cli` (rather than the
 * host platform's `ooniprobe-desktop`). Fed to [CoreConfig.engineName] and the geoip session config.
 */
const val CLI_ENGINE_NAME = "cli"

/**
 * Everything the desktop upload gateway needs, derived from the resolved CLI runtime so tests can
 * point it at temp paths and never touch real user data.
 */
data class CliEngineConfig(
    /** Directory holding `probe.db`. */
    val databaseDir: String,
    /** Base directory holding measurement report files (`Measurement/<id>_<test>.json`), state, tunnel, assets. */
    val baseFileDir: String,
    val cacheDir: String,
    val ooniApiBaseUrl: String,
    val baseSoftwareName: String,
    val softwareVersion: String,
    val passportVersion: String,
    val proxy: String?,
    val osName: String,
    val osVersion: String,
    /** Optional path to a geoip mmdb file; used by the run gateway's engine preferences. */
    val geoipDbPath: String? = null,
)
