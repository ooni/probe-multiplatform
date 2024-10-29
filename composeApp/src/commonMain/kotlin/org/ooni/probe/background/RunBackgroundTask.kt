package org.ooni.probe.background

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.TestRunState
import org.ooni.probe.domain.UploadMissingMeasurements
import org.ooni.probe.domain.UploadMissingMeasurements.State

class RunBackgroundTask(
    private val getPreferenceValueByKey: (SettingsKey) -> Flow<Any?>,
    private val uploadMissingMeasurements: (ResultModel.Id?) -> Flow<UploadMissingMeasurements.State>,
    private val checkSkipAutoRunNotUploadedLimit: () -> Flow<Boolean>,
    private val getAutoRunSpecification: suspend () -> RunSpecification,
    private val runDescriptors: suspend (RunSpecification) -> Unit,
    private val getCurrentTestState: () -> Flow<TestRunState>,
) {
    operator fun invoke(spec: RunSpecification?): Flow<State> =
        channelFlow {
            if (spec == null &&
                getPreferenceValueByKey(SettingsKey.UPLOAD_RESULTS).first() == true
            ) {
                uploadMissingMeasurements(null).collectLatest {
                    send(State.UploadingMissingResults(it))
                }
            }

            if (checkSkipAutoRunNotUploadedLimit().first()) {
                Logger.i("Skipping auto-run tests due to not uploaded results limit")
                return@channelFlow
            }

            coroutineScope {
                val runJob = async {
                    runDescriptors(spec ?: getAutoRunSpecification())
                }

                var testStarted = false
                getCurrentTestState()
                    .takeWhile { state ->
                        state is TestRunState.Running || (state is TestRunState.Idle && !testStarted)
                    }
                    .onEach { state ->
                        if (state !is TestRunState.Running) return@onEach
                        testStarted = true
                        send(State.RunningTests(state))
                    }
                    .collect()

                runJob.await()
            }
        }

    sealed interface State {
        data class UploadingMissingResults(val state: UploadMissingMeasurements.State) : State

        data class RunningTests(val state: TestRunState.Running) : State
    }
}
