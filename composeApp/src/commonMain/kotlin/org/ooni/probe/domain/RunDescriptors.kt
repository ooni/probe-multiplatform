package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.EnginePreferences
import org.ooni.engine.models.Result
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.TestRunError
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.data.models.UrlModel
import org.ooni.probe.shared.monitoring.Instrumentation
import org.ooni.probe.shared.now
import kotlin.time.Duration

class RunDescriptors(
    private val getTestDescriptorsBySpec: suspend (RunSpecification.Full) -> List<Descriptor>,
    private val downloadUrls: suspend (TaskOrigin) -> Result<List<UrlModel>, MkException>,
    private val storeResult: suspend (ResultModel) -> ResultModel.Id,
    private val markResultAsDone: suspend (ResultModel.Id) -> Unit,
    private val getRunBackgroundState: Flow<RunBackgroundState>,
    private val setRunBackgroundState: ((RunBackgroundState) -> RunBackgroundState) -> Unit,
    private val runNetTest: suspend (RunNetTest.Specification) -> Unit,
    private val addRunCancelListener: (() -> Unit) -> Unit,
    private val reportTestRunError: (TestRunError) -> Unit,
    private val getEnginePreferences: suspend () -> EnginePreferences,
    private val finishInProgressData: suspend () -> Unit,
) {
    suspend operator fun invoke(spec: RunSpecification.Full) {
        Instrumentation.withTransaction(
            operation = this::class.simpleName.orEmpty(),
            data = mapOf(
                "taskOrigin" to spec.taskOrigin.value,
                "isRerun" to spec.isRerun,
                "testsCount" to spec.tests.size,
            ),
        ) {
            setRunBackgroundState { RunBackgroundState.RunningTests() }

            val descriptors = getTestDescriptorsBySpec(spec)
            val descriptorsWithFinalInputs = descriptors.prepareInputs(spec.taskOrigin)
            val estimatedRuntime = descriptorsWithFinalInputs.getEstimatedRuntime()

            setRunBackgroundState {
                RunBackgroundState.RunningTests(estimatedRuntimeOfDescriptors = estimatedRuntime)
            }

            try {
                runDescriptorsCancellable(descriptorsWithFinalInputs, spec)
            } catch (e: Exception) {
                // Exceptions were logged in the Engine
            } finally {
                setRunBackgroundState { RunBackgroundState.Idle(LocalDateTime.now(), true) }
                finishInProgressData()
            }
        }
    }

    private suspend fun runDescriptorsCancellable(
        descriptors: List<Descriptor>,
        spec: RunSpecification.Full,
    ) {
        addRunCancelListener {
            setRunBackgroundState { RunBackgroundState.Stopping }
        }

        // Actually running the descriptors
        descriptors.forEachIndexed { index, descriptor ->
            if (isRunStopped()) return@forEachIndexed
            runDescriptor(descriptor, index, spec.taskOrigin, spec.isRerun)
        }
    }

    private suspend fun List<Descriptor>.prepareInputs(taskOrigin: TaskOrigin) =
        map { descriptor ->
            descriptor.copy(
                netTests = descriptor.netTests.downloadUrlsIfNeeded(taskOrigin, descriptor),
                longRunningTests = descriptor.longRunningTests.downloadUrlsIfNeeded(taskOrigin, descriptor),
            )
        }
            .filterNot { it.allTests.isEmpty() }

    private suspend fun List<NetTest>.downloadUrlsIfNeeded(
        taskOrigin: TaskOrigin,
        descriptor: Descriptor,
    ): List<NetTest> =
        map { test ->
            val urls = test.inputsOrDownloadUrls(taskOrigin, descriptor)
            test.copy(inputs = urls, callCheckIn = test.inputs != urls)
        }
            .filterNot { it.test is TestType.WebConnectivity && it.inputs?.any() != true }

    private suspend fun NetTest.inputsOrDownloadUrls(
        taskOrigin: TaskOrigin,
        descriptor: Descriptor,
    ): List<String>? {
        if (!inputs.isNullOrEmpty() || test !is TestType.WebConnectivity) return inputs

        // NOTE: General assumption here is that web_connectivity tests will run first.
        setRunBackgroundState {
            if (it !is RunBackgroundState.RunningTests) return@setRunBackgroundState it
            it.copy(
                descriptor = descriptor,
                testType = test,
            )
        }
        val urls = downloadUrls(taskOrigin)
            .onFailure { Logger.w("Could not download urls", it) }
            .get()
            ?.map { it.url }
            ?: emptyList()

        if (urls.isEmpty()) {
            reportTestRunError(TestRunError.DownloadUrlsFailed)
        }

        return urls
    }

    private suspend fun List<Descriptor>.getEstimatedRuntime(): List<Duration> {
        val maxRuntime = getEnginePreferences().maxRuntime
        return map { descriptor ->
            descriptor.estimatedDuration.coerceAtMost(maxRuntime ?: Duration.INFINITE)
        }
    }

    private suspend fun runDescriptor(
        descriptor: Descriptor,
        index: Int,
        taskOrigin: TaskOrigin,
        isRerun: Boolean,
    ) {
        val result = ResultModel(
            descriptorName = descriptor.name,
            descriptorKey = (descriptor.source as? Descriptor.Source.Installed)?.value?.key,
            taskOrigin = taskOrigin,
        )
        val resultId = storeResult(result)

        descriptor.allTests.forEachIndexed { testIndex, netTest ->
            if (isRunStopped()) return@forEachIndexed
            runNetTest(
                RunNetTest.Specification(
                    descriptor = descriptor,
                    descriptorIndex = index,
                    netTest = netTest,
                    taskOrigin = taskOrigin,
                    isRerun = isRerun,
                    resultId = resultId,
                    testIndex = testIndex,
                    testTotal = descriptor.allTests.size,
                ),
            )
        }

        markResultAsDone(resultId)
    }

    private suspend fun isRunStopped() = getRunBackgroundState.first() is RunBackgroundState.Stopping
}
