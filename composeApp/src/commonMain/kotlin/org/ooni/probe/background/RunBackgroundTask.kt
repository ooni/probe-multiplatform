package org.ooni.probe.background

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.ooni.engine.models.NetworkType
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.domain.UploadMissingMeasurements

class RunBackgroundTask(
    private val getPreferenceValueByKey: (SettingsKey) -> Flow<Any?>,
    private val uploadMissingMeasurements: (ResultModel.Id?) -> Flow<UploadMissingMeasurements.State>,
    private val checkSkipAutoRunNotUploadedLimit: () -> Flow<Boolean>,
    private val getNetworkType: () -> NetworkType,
    private val getAutoRunSpecification: suspend () -> RunSpecification,
    private val runDescriptors: suspend (RunSpecification) -> Unit,
    private val setRunBackgroundState: ((RunBackgroundState) -> RunBackgroundState) -> Unit,
    private val getRunBackgroundState: () -> Flow<RunBackgroundState>,
    private val addRunCancelListener: (() -> Unit) -> Unit,
    private val clearRunCancelListeners: () -> Unit,
) {
    operator fun invoke(spec: RunSpecification?): Flow<RunBackgroundState> =
        channelFlow {
            val initialState = getRunBackgroundState().first()
            if (initialState !is RunBackgroundState.Idle) {
                Logger.i("Background task is already running, so we won't start another one")
                return@channelFlow
            }

            var isCancelled = false

            if (spec == null &&
                getPreferenceValueByKey(SettingsKey.UPLOAD_RESULTS).first() == true
            ) {
                supervisorScope {
                    val uploadJob = async {
                        uploadMissingMeasurements(null)
                            .collectLatest { uploadState ->
                                setRunBackgroundState {
                                    RunBackgroundState.UploadingMissingResults(uploadState)
                                }
                                send(RunBackgroundState.UploadingMissingResults(uploadState))
                            }
                    }

                    addRunCancelListener {
                        isCancelled = true
                        if (uploadJob.isActive) uploadJob.cancel()
                        setRunBackgroundState { RunBackgroundState.Stopping }
                        CoroutineScope(Dispatchers.Default).launch { send(RunBackgroundState.Stopping) }
                    }

                    try {
                        uploadJob.await()
                    } catch (e: CancellationException) {
                        Logger.i("Upload Missing Results: cancelled")
                    }
                }
            }

            if (isCancelled) {
                setRunBackgroundState { RunBackgroundState.Idle() }
                send(RunBackgroundState.Idle())
                return@channelFlow
            }

            if (checkSkipAutoRunNotUploadedLimit().first()) {
                Logger.i("Skipping auto-run tests: too many not-uploaded results")
                return@channelFlow
            }

            if (getNetworkType() == NetworkType.VPN) {
                Logger.i("Skipping auto-run tests: VPN enabled")
                return@channelFlow
            }

            coroutineScope {
                val runJob = async {
                    runDescriptors(spec ?: getAutoRunSpecification())
                }

                var testStarted = false
                getRunBackgroundState()
                    .takeWhile { state ->
                        state is RunBackgroundState.RunningTests ||
                            state is RunBackgroundState.Stopping ||
                            (state is RunBackgroundState.Idle && !testStarted)
                    }
                    .onEach { state ->
                        if (state is RunBackgroundState.Idle) return@onEach
                        testStarted = true
                        send(state)
                    }
                    .collect()

                runJob.await()
            }
        }.onCompletion {
            clearRunCancelListeners()
        }
}
