package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import org.ooni.engine.models.TaskEvent
import org.ooni.engine.models.TaskEventResult
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.disk.DeleteFiles
import org.ooni.probe.data.disk.WriteFile
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.data.models.UrlModel
import org.ooni.probe.shared.monitoring.Instrumentation
import org.ooni.probe.shared.toLocalDateTime

class RunNetTest(
    private val startTest: (NetTest, TaskOrigin, InstalledTestDescriptorModel.Id?) -> Flow<TaskEvent>,
    private val getOrCreateUrl: suspend (String) -> UrlModel,
    private val storeMeasurement: suspend (MeasurementModel) -> MeasurementModel.Id,
    private val storeNetwork: suspend (NetworkModel) -> NetworkModel.Id,
    private val getResultByIdAndUpdate: suspend (ResultModel.Id, (ResultModel) -> ResultModel) -> Unit,
    private val setCurrentTestState: ((RunBackgroundState) -> RunBackgroundState) -> Unit,
    private val writeFile: WriteFile,
    private val deleteFiles: DeleteFiles,
    private val json: Json,
    private val spec: Specification,
) {
    data class Specification(
        val descriptor: Descriptor,
        val descriptorIndex: Int,
        val netTest: NetTest,
        val taskOrigin: TaskOrigin,
        val isRerun: Boolean,
        val resultId: ResultModel.Id,
        val testIndex: Int,
        val testTotal: Int,
    )

    private var reportId: String? = null
    private var lastNetwork: NetworkModel? = null
    private val measurements = mutableMapOf<Int, MeasurementModel>()
    private val progressStep = 1.0 / spec.descriptor.netTests.size

    suspend operator fun invoke() {
        Instrumentation.withTransaction(
            operation = this::class.simpleName.orEmpty(),
            data = mapOf(
                "test" to spec.netTest.test.name,
                "inputsCount" to (spec.netTest.inputs?.size ?: 0),
            ),
        ) {
            setCurrentTestState {
                if (it !is RunBackgroundState.RunningTests) return@setCurrentTestState it
                it.copy(
                    descriptor = spec.descriptor,
                    descriptorIndex = spec.descriptorIndex,
                    testType = spec.netTest.test,
                    testProgress = spec.testIndex * progressStep,
                    testIndex = spec.testIndex,
                    testTotal = spec.testTotal,
                )
            }
            val installedDescriptorId =
                (spec.descriptor.source as? Descriptor.Source.Installed)?.value?.id

            startTest(
                spec.netTest,
                spec.taskOrigin,
                installedDescriptorId,
            )
                .collect(::onEvent)
        }
    }

    private suspend fun onEvent(event: TaskEvent) {
        when (event) {
            TaskEvent.Started -> {
                // We already update the initial state before starting the task
            }

            is TaskEvent.GeoIpLookup -> {
                val network = NetworkModel(
                    networkName = event.networkName,
                    asn = event.asn,
                    countryCode = event.countryCode,
                    networkType = event.networkType,
                )
                val networkId = storeNetwork(network)
                lastNetwork = network.copy(id = networkId)
                updateResult { it.copy(networkId = networkId) }
            }

            is TaskEvent.ReportCreate -> {
                reportId = event.reportId
            }

            is TaskEvent.MeasurementStart -> {
                createMeasurement(
                    event.index,
                    MeasurementModel(
                        test = spec.netTest.test,
                        reportId = reportId?.let(MeasurementModel::ReportId),
                        resultId = spec.resultId,
                        urlId = if (event.url.isNullOrEmpty()) {
                            null
                        } else {
                            getOrCreateUrl(event.url).id
                        },
                    ),
                )
            }

            is TaskEvent.Log -> {
                val knownException = event.getKnownException()
                if (knownException != null) {
                    Logger.w(event.message, knownException)
                } else {
                    Logger.log(
                        severity = when (event.level) {
                            "WARNING" -> Severity.Warn
                            "DEBUG" -> Severity.Debug
                            else -> Severity.Info
                        },
                        message = event.message,
                        throwable = null,
                        tag = Logger.tag,
                    )
                }

                setCurrentTestState {
                    if (it !is RunBackgroundState.RunningTests) return@setCurrentTestState it
                    it.copy(log = event.message)
                }
            }

            is TaskEvent.Progress -> {
                setCurrentTestState {
                    if (it !is RunBackgroundState.RunningTests) return@setCurrentTestState it
                    it.copy(
                        testProgress = (spec.testIndex + event.progress) * progressStep,
                        log = event.message,
                    )
                }
            }

            is TaskEvent.Measurement -> {
                updateMeasurement(event.index) { initialMeasurement ->
                    var measurement = initialMeasurement

                    if (event.result == null) {
                        measurement = measurement.copy(isFailed = true)
                    } else {
                        if (event.result.testStartTime != null) {
                            updateResult {
                                it.copy(
                                    startTime = event.result.testStartTime.toLocalDateTime(),
                                )
                            }
                        }
                        if (event.result.measurementStartTime != null) {
                            measurement = measurement.copy(
                                startTime = event.result.measurementStartTime.toLocalDateTime(),
                            )
                        }
                        if (event.result.testRuntime != null) {
                            measurement = measurement.copy(
                                runtime = event.result.testRuntime,
                            )
                        }

                        // Introduced to capture the URL for measurements (`echcheck`) which did not
                        // emmit the URL in the measurement start event.
                        // see https://github.com/ooni/probe-multiplatform/issues/435
                        if (event.result.input != null && measurement.urlId == null) {
                            measurement = measurement.copy(
                                urlId = getOrCreateUrl(event.result.input).id,
                            )
                        }

                        event.result.testKeys?.let {
                            val testKeys = extractTestKeysPropertiesToJson(it)
                            measurement = measurement.copy(
                                testKeys = if (testKeys.isEmpty()) {
                                    null
                                } else {
                                    json.encodeToString(testKeys)
                                },
                            )
                        }

                        val evaluation =
                            evaluateMeasurementKeys(spec.netTest.test, event.result.testKeys)
                        measurement = measurement.copy(
                            isFailed = evaluation.isFailed,
                            isAnomaly = evaluation.isAnomaly,
                        )
                    }

                    if (spec.isRerun && lastNetwork != null) {
                        measurement = measurement.copy(
                            rerunNetwork = json.encodeToString(lastNetwork),
                        )
                    }

                    writeToReportFile(measurement, event.json)

                    measurement
                }
            }

            is TaskEvent.MeasurementSubmissionSuccessful -> {
                updateMeasurement(event.index) { measurement ->
                    return@updateMeasurement if (lastNetwork?.isValid() != true) {
                        measurement.copy(
                            isFailed = true,
                            failureMessage = "Network is not valid",
                        )
                    } else if (event.measurementUid == null) {
                        measurement.copy(
                            isFailed = true,
                            failureMessage = "Submission failed: missing measurement UID",
                        )
                    } else {
                        measurement.reportFilePath?.let { deleteFiles(it) }
                        measurement.copy(
                            isUploaded = true,
                            uid = MeasurementModel.Uid(event.measurementUid),
                        )
                    }
                }
            }

            is TaskEvent.MeasurementSubmissionFailure -> {
                updateMeasurement(event.index) {
                    var measurement = it.copy(
                        isUploaded = false,
                        reportId = null,
                        isUploadFailed = true,
                    )
                    event.message?.let {
                        measurement = measurement.copy(failureMessage = it)
                    }
                    measurement
                }
            }

            is TaskEvent.MeasurementDone -> {
                updateMeasurement(event.index) {
                    it.copy(isDone = true)
                }
            }

            is TaskEvent.End -> {
                updateResult {
                    it.copy(
                        dataUsageDown = it.dataUsageDown + event.downloadedKb,
                        dataUsageUp = it.dataUsageUp + event.uploadedKb,
                    )
                }
            }

            is TaskEvent.StartupFailure,
            is TaskEvent.ResolverLookupFailure,
            -> {
                val message = when (event) {
                    is TaskEvent.StartupFailure -> event.message
                    is TaskEvent.ResolverLookupFailure -> event.message
                    else -> null
                }

                if (message != null) {
                    updateResult {
                        it.copy(
                            failureMessage =
                                if (it.failureMessage != null) {
                                    "${it.failureMessage}\n$message"
                                } else {
                                    message
                                },
                        )
                    }
                }

                if (event.isCancelled() == true) return

                when (event) {
                    is TaskEvent.StartupFailure ->
                        Logger.w("", StartupFailure(message, event.value))

                    is TaskEvent.ResolverLookupFailure ->
                        Logger.i("", ResolverLookupFailure(message, event.value))

                    else -> Unit
                }
            }

            is TaskEvent.BugJsonDump -> {
                Logger.w("", BugJsonDump(event.value))
            }

            is TaskEvent.TaskTerminated -> Unit
        }
    }

    private suspend fun updateResult(update: (ResultModel) -> ResultModel) {
        getResultByIdAndUpdate(spec.resultId, update)
    }

    private suspend fun createMeasurement(
        index: Int,
        measurement: MeasurementModel,
    ) {
        measurements[index] =
            measurement.copy(id = storeMeasurement(measurement))
    }

    private suspend fun updateMeasurement(
        index: Int,
        update: suspend (MeasurementModel) -> MeasurementModel,
    ) {
        val measurement = measurements[index] ?: return
        val updatedMeasurement = update(measurement)
        measurements[index] = updatedMeasurement
        storeMeasurement(updatedMeasurement)
    }

    private suspend fun writeToReportFile(
        measurement: MeasurementModel,
        text: String,
    ) {
        writeFile(
            path = measurement.reportFilePath ?: return,
            contents = text,
            append = false,
        )
    }

    /**
     * Some log warnings are known, but come with a different warning message for each instance.
     * Here we group them under a single exception class for crash reporting.
     */
    private fun TaskEvent.Log.getKnownException() =
        if (
            message.startsWith("cannot submit measurement") &&
            listOf(
                "interrupted", "generic_timeout_error", "connection_aborted", "connection_reset",
                "eof_error",
            ).none { message.contains(it) }
        ) {
            CannotSubmitMeasurement()
        } else if (message.startsWith("statsManager") && message.contains("not found:")) {
            StatsManagerNotFoundError()
        } else {
            null
        }

    open inner class Failure(message: String?, value: TaskEventResult.Value?) :
        Exception(message ?: value?.let(json::encodeToString))

    inner class StartupFailure(message: String?, value: TaskEventResult.Value?) :
        Failure(message, value)

    inner class ResolverLookupFailure(message: String?, value: TaskEventResult.Value?) :
        Failure(message, value)

    inner class BugJsonDump(value: TaskEventResult.Value?) : Failure(null, value)

    inner class CannotSubmitMeasurement : Failure(null, null)

    inner class StatsManagerNotFoundError : Failure(null, null)
}
