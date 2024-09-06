package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ooni.engine.models.TaskEvent
import org.ooni.engine.models.TaskEventResult
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.disk.DeleteFile
import org.ooni.probe.data.disk.WriteFile
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.TestRunState
import org.ooni.probe.data.models.UrlModel
import org.ooni.probe.shared.toLocalDateTime

class RunNetTest(
    private val startTest: (String, List<String>?, TaskOrigin, InstalledTestDescriptorModel.Id?) -> Flow<TaskEvent>,
    private val getUrlByUrl: suspend (String) -> Flow<UrlModel?>,
    private val storeMeasurement: suspend (MeasurementModel) -> MeasurementModel.Id,
    private val storeNetwork: suspend (NetworkModel) -> NetworkModel.Id,
    private val storeResult: suspend (ResultModel) -> ResultModel.Id,
    private val setCurrentTestState: ((TestRunState) -> TestRunState) -> Unit,
    private val writeFile: WriteFile,
    private val deleteFile: DeleteFile,
    private val json: Json,
    private val spec: Specification,
) {
    data class Specification(
        val descriptor: Descriptor,
        val descriptorIndex: Int,
        val netTest: NetTest,
        val taskOrigin: TaskOrigin,
        val isRerun: Boolean,
        val initialResult: ResultModel,
        val testIndex: Int,
        val testTotal: Int,
    )

    private var result = spec.initialResult
    private var reportId: String? = null
    private var lastNetwork: NetworkModel? = null
    private val measurements = mutableMapOf<Int, MeasurementModel>()
    private val progressStep = 1.0 / spec.descriptor.netTests.size

    suspend operator fun invoke() {
        setCurrentTestState {
            if (it !is TestRunState.Running) return@setCurrentTestState it
            it.copy(
                descriptor = spec.descriptor,
                descriptorIndex = spec.descriptorIndex,
                testType = spec.netTest.test,
                testProgress = spec.testIndex * progressStep,
                testIndex = spec.testIndex,
                testTotal = spec.testTotal,
            )
        }

        when (spec.descriptor.source) {
            is Descriptor.Source.Default -> {
                startTest(spec.netTest.test.name, spec.netTest.inputs, spec.taskOrigin, null)
                    .collect(::onEvent)
            }
            is Descriptor.Source.Installed -> {
                startTest(spec.netTest.test.name, spec.netTest.inputs, spec.taskOrigin, spec.descriptor.source.value.id)
                    .collect(::onEvent)
            }
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
                    ip = event.ip,
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
                        resultId = result.id ?: return,
                        urlId = if (event.url.isNullOrEmpty()) {
                            null
                        } else {
                            getUrlByUrl(event.url).first()?.id
                        },
                    ),
                )
            }

            is TaskEvent.Log -> {
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

                setCurrentTestState {
                    if (it !is TestRunState.Running) return@setCurrentTestState it
                    it.copy(log = event.message)
                }

                writeToLogFile(event.message + "\n")
            }

            is TaskEvent.Progress -> {
                setCurrentTestState {
                    if (it !is TestRunState.Running) return@setCurrentTestState it
                    it.copy(
                        testProgress = (spec.testIndex + event.progress) * progressStep,
                        log = event.message,
                    )
                }

                writeToLogFile(event.message + "\n")
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
                        // We ignore test_keys because we're no longer storing them
                        // TODO: specific tests add more fields (see AbstractTest.onEntry)
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
                updateMeasurement(event.index) {
                    it.copy(isUploaded = true)
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
                        dataUsageDown = result.dataUsageDown + event.downloadedKb,
                        dataUsageUp = result.dataUsageUp + event.uploadedKb,
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
                                if (result.failureMessage != null) {
                                    "${result.failureMessage}\n\n$message"
                                } else {
                                    message
                                },
                        )
                    }
                }

                val value = when (event) {
                    is TaskEvent.StartupFailure -> event.value
                    is TaskEvent.ResolverLookupFailure -> event.value
                    else -> null
                } ?: return

                Logger.w(message ?: "Failure", Failure(value))
            }

            is TaskEvent.BugJsonDump -> {
                Logger.w("BugJsonDump", Failure(event.value))
            }

            is TaskEvent.TaskTerminated -> {
                val measurement = measurements[event.index] ?: return
                if (measurement.isUploaded) {
                    measurement.reportFilePath?.let { deleteFile(it) }
                }
            }
        }
    }

    private suspend fun updateResult(update: (ResultModel) -> ResultModel) {
        result = update(result)
        storeResult(result)
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

    private suspend fun writeToLogFile(text: String) {
        writeFile(
            path = MeasurementModel.logFilePath(result.idOrThrow, spec.netTest.test),
            contents = text,
            append = true,
        )
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

    inner class Failure(value: TaskEventResult.Value) : Exception(json.encodeToString(value))
}
