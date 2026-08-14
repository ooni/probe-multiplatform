package org.ooni.probe.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.ooni.probe.background.RunBackgroundTask
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.domain.CancelListenerCallback
import org.ooni.probe.domain.UploadMissingMeasurements

/**
 * CLI-facing run surface: drives the canonical run/upload orchestration
 * ([RunBackgroundTask] -> [org.ooni.probe.domain.RunDescriptors] -> [org.ooni.probe.domain.RunNetTest])
 * and projects [RunBackgroundState] into a UI-free [CliRunProgress] stream.
 *
 * This is the ONLY exposed run entry point: CLI commands (T9) must go through this gateway rather
 * than constructing engine tasks or repositories directly.
 */
interface CliRunGateway {
    fun run(
        spec: RunSpecification,
        options: CliRunOptions = CliRunOptions(),
    ): Flow<CliRunProgress>

    /** Bootstrap descriptors available to run (the bundled OONI descriptor set for the CLI). */
    suspend fun descriptors(): List<Descriptor>

    /**
     * Cancels an in-flight [run] through the canonical cancellation path (fires the registered
     * run-cancel listeners so the run flow completes gracefully with cleanup preserved). Safe to
     * call when no run is active.
     */
    fun cancel()

    fun close()
}

/**
 * CLI run modifiers mirroring probe-cli's flags.
 *
 * - [noCollector]: do not upload measurements to the collector; measurements stay stored locally
 *   as not-uploaded. Reflected in the engine's `TaskSettings`/`EnginePreferences.uploadResults`.
 * - [noCreds]: never prepare anonymous credentials, never read/write secure-storage credentials,
 *   and route submission through the anonymous engine-submit path. If a collector requires a
 *   credential, the measurement remains not-uploaded rather than silently registering one.
 */
data class CliRunOptions(
    val noCollector: Boolean = false,
    val noCreds: Boolean = false,
)

/** UI-free projection of [RunBackgroundState]; mirrors the style of [CliUploadProgress]. */
data class CliRunProgress(
    val phase: Phase,
    val progress: Double = 0.0,
    val descriptorName: String? = null,
    val testType: String? = null,
    val log: String? = null,
    val uploaded: Int = 0,
    val failedToUpload: Int = 0,
    val total: Int = 0,
    val finished: Boolean = false,
) {
    enum class Phase {
        Preparing,
        RunningTests,
        UploadingMissingResults,
        Stopping,
        Idle,
    }

    companion object {
        fun from(state: RunBackgroundState): CliRunProgress =
            when (state) {
                RunBackgroundState.Idle -> CliRunProgress(Phase.Idle, finished = true)
                RunBackgroundState.Preparing -> CliRunProgress(Phase.Preparing)
                RunBackgroundState.Stopping -> CliRunProgress(Phase.Stopping)
                is RunBackgroundState.RunningTests ->
                    CliRunProgress(
                        phase = Phase.RunningTests,
                        progress = state.progress,
                        descriptorName = state.descriptor?.name,
                        testType = state.testType?.name,
                        log = state.log,
                    )
                is RunBackgroundState.UploadingMissingResults ->
                    when (val upload = state.state) {
                        UploadMissingMeasurements.State.Starting ->
                            CliRunProgress(Phase.UploadingMissingResults)
                        is UploadMissingMeasurements.State.Uploading ->
                            CliRunProgress(
                                phase = Phase.UploadingMissingResults,
                                uploaded = upload.uploaded,
                                failedToUpload = upload.failedToUpload,
                                total = upload.total,
                            )
                        is UploadMissingMeasurements.State.Finished ->
                            CliRunProgress(
                                phase = Phase.UploadingMissingResults,
                                uploaded = upload.uploaded,
                                failedToUpload = upload.failedToUpload,
                                total = upload.total,
                            )
                    }
            }
    }
}

/**
 * Pure, injectable mapping unit: wraps the canonical [RunBackgroundTask] and maps its
 * `Flow<RunBackgroundState>` into `Flow<CliRunProgress>`, applying [CliRunOptions].
 *
 * It takes the same injectable lambdas [RunBackgroundTask] needs so tests can exercise the full
 * run/upload semantics with fakes (no engine or passport native library required).
 *
 * `--no-creds` swaps in a no-op `prepareAnonymousCredentials`; the anonymous submit routing and the
 * `--no-collector` engine/`TaskSettings` reflection are applied by the caller that owns the engine
 * wiring (see `DesktopCliRunGateway`).
 */
class CliRunOrchestrator(
    private val getPreferenceValueByKey: (SettingsKey) -> Flow<Any?>,
    private val prepareAnonymousCredentials: suspend () -> Unit,
    private val uploadMissingMeasurements: (MeasurementsFilter) -> Flow<UploadMissingMeasurements.State>,
    private val runDescriptors: suspend (RunSpecification.Full) -> Unit,
    private val getRerunSpecification: suspend (RunSpecification.Rerun) -> RunSpecification.Full?,
    private val setRunBackgroundState: ((RunBackgroundState) -> RunBackgroundState) -> Unit,
    private val getRunBackgroundState: () -> Flow<RunBackgroundState>,
    private val addRunCancelListener: (() -> Unit) -> CancelListenerCallback,
) {
    fun run(
        spec: RunSpecification,
        options: CliRunOptions = CliRunOptions(),
    ): Flow<CliRunProgress> {
        val task = RunBackgroundTask(
            getPreferenceValueByKey = getPreferenceValueByKey,
            prepareAnonymousCredentials = if (options.noCreds) NO_OP_CREDENTIALS else prepareAnonymousCredentials,
            uploadMissingMeasurements = uploadMissingMeasurements,
            // CLI runs always carry an explicit spec, so the autorun-only paths are never reached.
            checkAutoRunConstraints = { true },
            getAutoRunSpecification = { throw UnsupportedOperationException("CLI run requires an explicit RunSpecification") },
            getRerunSpecification = getRerunSpecification,
            runDescriptors = runDescriptors,
            setRunBackgroundState = setRunBackgroundState,
            getRunBackgroundState = getRunBackgroundState,
            addRunCancelListener = addRunCancelListener,
        )
        return task(spec)
            .map(CliRunProgress::from)
            // RunBackgroundTask swallows the final Idle state, so emit a terminal finished marker
            // on normal completion (mirrors CliUploadProgress.finished).
            .onCompletion { cause ->
                if (cause == null) emit(CliRunProgress(CliRunProgress.Phase.Idle, finished = true))
            }
    }

    private companion object {
        val NO_OP_CREDENTIALS: suspend () -> Unit = {}
    }
}
