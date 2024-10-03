package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
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
import org.ooni.probe.data.models.TestRunState
import org.ooni.probe.data.models.UrlModel
import org.ooni.probe.shared.now
import kotlin.time.Duration

class RunDescriptors(
    private val getTestDescriptorsBySpec: suspend (RunSpecification) -> List<Descriptor>,
    private val downloadUrls: suspend (TaskOrigin) -> Result<List<UrlModel>, MkException>,
    private val storeResult: suspend (ResultModel) -> ResultModel.Id,
    private val markResultAsDone: suspend (ResultModel.Id) -> Unit,
    private val getCurrentTestRunState: Flow<TestRunState>,
    private val setCurrentTestState: ((TestRunState) -> TestRunState) -> Unit,
    private val runNetTest: suspend (RunNetTest.Specification) -> Unit,
    private val observeCancelTestRun: Flow<Unit>,
    private val reportTestRunError: (TestRunError) -> Unit,
    private val getEnginePreferences: suspend () -> EnginePreferences,
    private val finishInProgressData: suspend () -> Unit,
) {
    suspend operator fun invoke(spec: RunSpecification) {
        val descriptors = getTestDescriptorsBySpec(spec)
        val descriptorsWithFinalInputs = descriptors.prepareInputs(spec.taskOrigin)
        val estimatedRuntime = descriptorsWithFinalInputs.getEstimatedRuntime()

        if (getCurrentTestRunState.first() is TestRunState.Running) {
            Logger.i("Tests are already running, so we won't run other tests")
            return
        }
        setCurrentTestState {
            TestRunState.Running(estimatedRuntimeOfDescriptors = estimatedRuntime)
        }

        try {
            runDescriptorsCancellable(descriptorsWithFinalInputs, spec)
        } catch (e: Exception) {
            // Exceptions were logged in the Engine
        } finally {
            setCurrentTestState { TestRunState.Idle(LocalDateTime.now(), true) }
            finishInProgressData()
        }
    }

    private suspend fun runDescriptorsCancellable(
        descriptors: List<Descriptor>,
        spec: RunSpecification,
    ) {
        coroutineScope {
            val runJob = async {
                // Actually running the descriptors
                descriptors.forEachIndexed { index, descriptor ->
                    runDescriptor(descriptor, index, spec.taskOrigin, spec.isRerun)
                }
            }
            // Observe if a cancel request has been made
            val cancelJob = async {
                observeCancelTestRun
                    .take(1)
                    .collect {
                        setCurrentTestState { TestRunState.Stopping }
                        runJob.cancel()
                    }
            }

            runJob.await()

            if (cancelJob.isActive) {
                cancelJob.cancel()
            }
        }
    }

    private suspend fun List<Descriptor>.prepareInputs(taskOrigin: TaskOrigin) =
        map { descriptor ->
            descriptor.copy(
                netTests = descriptor.netTests.downloadUrlsIfNeeded(taskOrigin),
                longRunningTests = descriptor.longRunningTests.downloadUrlsIfNeeded(taskOrigin),
            )
        }
            .filterNot { it.allTests.isEmpty() }

    private suspend fun List<NetTest>.downloadUrlsIfNeeded(taskOrigin: TaskOrigin): List<NetTest> =
        map { test -> test.copy(inputs = test.inputsOrDownloadUrls(taskOrigin)) }
            .filterNot { it.test is TestType.WebConnectivity && it.inputs?.any() != true }

    private suspend fun NetTest.inputsOrDownloadUrls(taskOrigin: TaskOrigin): List<String>? {
        if (!inputs.isNullOrEmpty() || test !is TestType.WebConnectivity) return inputs

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
            testGroupName = descriptor.name,
            testDescriptorId = (descriptor.source as? Descriptor.Source.Installed)?.value?.id,
            taskOrigin = taskOrigin,
        )
        val resultId = storeResult(result)

        descriptor.allTests.forEachIndexed { testIndex, netTest ->
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
}
