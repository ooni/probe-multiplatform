package org.ooni.probe.background

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ProducerScope
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
    private val getLatestResult: () -> Flow<ResultModel?>,
) {
    operator fun invoke(spec: RunSpecification?): Flow<RunBackgroundState> =
        channelFlow {
            val initialState = getRunBackgroundState().first()
            if (initialState !is RunBackgroundState.Idle) {
                Logger.i("Background task is already running, so we won't start another one")
                return@channelFlow
            }

            val uploadCancelled = uploadMissingResults(isAutoRun = spec == null)
            if (uploadCancelled) return@channelFlow

            runTests(spec)

            // When a test is cancelled, sometimes the last measurement isn't uploaded

            getLatestResult().first()?.id.let { latestResultId ->
                val idleState = getRunBackgroundState().first()
                uploadMissingResults(isAutoRun = spec == null, resultId = latestResultId)
                updateState(idleState)
            }
        }.onCompletion {
            clearRunCancelListeners()
        }

    private suspend fun ProducerScope<RunBackgroundState>.uploadMissingResults(
        isAutoRun: Boolean,
        resultId: ResultModel.Id? = null,
    ): Boolean {
        val autoUpload = getPreferenceValueByKey(SettingsKey.UPLOAD_RESULTS).first() == true
        var isCancelled = false

        if ((isAutoRun || resultId != null) && autoUpload) {
            coroutineScope {
                val uploadJob = async {
                    uploadMissingMeasurements(resultId)
                        .collectLatest { uploadState ->
                            updateState(RunBackgroundState.UploadingMissingResults(uploadState))
                        }
                }

                addRunCancelListener {
                    isCancelled = true
                    if (uploadJob.isActive) uploadJob.cancel()
                    CoroutineScope(Dispatchers.Default).launch {
                        updateState(RunBackgroundState.Stopping)
                    }
                }

                try {
                    uploadJob.await()
                } catch (e: CancellationException) {
                    Logger.i("Upload Missing Results (result=$resultId): cancelled")
                }
            }
        }

        if (isCancelled) {
            updateState(RunBackgroundState.Idle())
            return true
        }

        return false
    }

    private suspend fun ProducerScope<RunBackgroundState>.runTests(spec: RunSpecification?) {
        if (checkSkipAutoRunNotUploadedLimit().first()) {
            Logger.i("Skipping auto-run tests: too many not-uploaded results")
            return
        }

        if (getNetworkType() == NetworkType.VPN && spec == null) {
            Logger.i("Skipping auto-run tests: VPN enabled")
            return
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
    }

    private suspend fun ProducerScope<RunBackgroundState>.updateState(state: RunBackgroundState) {
        setRunBackgroundState { state }
        send(state)
    }
}
