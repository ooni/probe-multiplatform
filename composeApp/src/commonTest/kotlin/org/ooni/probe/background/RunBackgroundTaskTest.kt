package org.ooni.probe.background

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.domain.CancelListenerCallback
import org.ooni.probe.domain.UploadMissingMeasurements
import kotlin.test.Test
import kotlin.test.assertFalse

class RunBackgroundTaskTest {
    @Test
    fun skipIfFailedAutoRunConstraints() =
        runTest {
            var wasRunDescriptorsCalled = false
            val state = MutableStateFlow<RunBackgroundState>(RunBackgroundState.Idle())
            val subject = buildSubject(
                checkAutoRunConstraints = { false },
                runDescriptors = {
                    wasRunDescriptorsCalled = true
                    state.value = RunBackgroundState.RunningTests()
                    delay(100)
                    state.value = RunBackgroundState.Idle()
                },
            )

            subject(null).collect()

            assertFalse(wasRunDescriptorsCalled)
        }

    private fun buildSubject(
        getPreferenceValueByKey: (SettingsKey) -> Flow<Any?> = { flowOf(true) },
        uploadMissingMeasurements: (MeasurementsFilter) -> Flow<UploadMissingMeasurements.State> = { emptyFlow() },
        checkAutoRunConstraints: suspend () -> Boolean = { false },
        getAutoRunSpecification: suspend () -> RunSpecification.Full = {
            RunSpecification.Full(
                tests = emptyList(),
                taskOrigin = TaskOrigin.AutoRun,
                isRerun = false,
            )
        },
        runDescriptors: suspend (RunSpecification) -> Unit = {},
        setRunBackgroundState: ((RunBackgroundState) -> RunBackgroundState) -> Unit = {},
        getRunBackgroundState: () -> Flow<RunBackgroundState> = { flowOf(RunBackgroundState.Idle()) },
        addRunCancelListener: (() -> Unit) -> CancelListenerCallback = { CancelListenerCallback {} },
        getLatestResult: () -> Flow<ResultModel?> = { flowOf(null) },
    ) = RunBackgroundTask(
        getPreferenceValueByKey = getPreferenceValueByKey,
        uploadMissingMeasurements = uploadMissingMeasurements,
        checkAutoRunConstraints = checkAutoRunConstraints,
        getAutoRunSpecification = getAutoRunSpecification,
        runDescriptors = runDescriptors,
        setRunBackgroundState = setRunBackgroundState,
        getRunBackgroundState = getRunBackgroundState,
        addRunCancelListener = addRunCancelListener,
        getLatestResult = getLatestResult,
    )
}
