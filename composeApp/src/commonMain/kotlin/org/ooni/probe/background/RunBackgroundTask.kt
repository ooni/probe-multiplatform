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
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.domain.UploadMissingMeasurements

class RunBackgroundTask(
    private val getPreferenceValueByKey: (SettingsKey) -> Flow<Any?>,
    private val uploadMissingMeasurements: (ResultModel.Id?) -> Flow<UploadMissingMeasurements.State>,
    private val checkAutoRunConstraints: suspend () -> Boolean,
    private val getAutoRunSpecification: suspend () -> RunSpecification.Full,
    private val runDescriptors: suspend (RunSpecification.Full) -> Unit,
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

            val isAutoRun = spec == null

            if (isAutoRun && !checkAutoRunConstraints()) return@channelFlow

            if (isAutoRun || spec is RunSpecification.OnlyUploadMissingResults) {
                val uploadCancelled = uploadMissingResults()
                if (uploadCancelled) return@channelFlow
            }

            if (spec is RunSpecification.OnlyUploadMissingResults) {
                setRunBackgroundState { RunBackgroundState.Idle() }
                return@channelFlow
            }

            runTests(spec as? RunSpecification.Full)

            // When a test is cancelled, sometimes the last measurement isn't uploaded

            getLatestResult().first()?.id.let { latestResultId ->
                val idleState = getRunBackgroundState().first()
                uploadMissingResults(resultId = latestResultId)
                updateState(idleState)
            }
        }.onCompletion {
            clearRunCancelListeners()
        }

    private suspend fun ProducerScope<RunBackgroundState>.uploadMissingResults(resultId: ResultModel.Id? = null): Boolean {
        val autoUpload = getPreferenceValueByKey(SettingsKey.UPLOAD_RESULTS).first() == true
        if (!autoUpload) return false

        var isCancelled = false

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

        if (isCancelled) {
            updateState(RunBackgroundState.Idle())
            return true
        }

        return false
    }

    private suspend fun ProducerScope<RunBackgroundState>.runTests(spec: RunSpecification.Full?) {
        coroutineScope {
            val runJob = async {
                runDescriptors(spec ?: getAutoRunSpecification())
            }

            var testStarted = false
            getRunBackgroundState()
                .takeWhile { state ->
                    state is RunBackgroundState.RunningTests ||
                        state is RunBackgroundState.UploadingMissingResults ||
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
