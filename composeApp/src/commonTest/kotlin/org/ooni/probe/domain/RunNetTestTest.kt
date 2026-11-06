package org.ooni.probe.domain

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.TaskEvent
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunModel
import org.ooni.probe.data.models.TestRunError
import org.ooni.probe.di.Dependencies
import org.ooni.testing.factories.DescriptorFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunNetTestTest {
    @Test
    fun geoIpLookupWithAsnZeroStopsTestAndUpdatesResult() =
        runTest {
            var failureMessage: String? = null
            val descriptor = DescriptorFactory.buildDescriptorWithInstalled(
                netTests = listOf(NetTest(TestType.WebConnectivity)),
            )

            val spec = RunNetTest.Specification(
                descriptor = descriptor,
                descriptorIndex = 0,
                netTest = descriptor.netTests.first(),
                taskOrigin = TaskOrigin.OoniRun,
                isRerun = false,
                resultId = ResultModel.Id(1),
                testIndex = 0,
                testTotal = 1,
            )

            val subject = RunNetTest(
                startTest = { _, _, _ ->
                    flowOf(
                        TaskEvent.GeoIpLookup(
                            networkName = "Network",
                            ip = "1.1.1.1",
                            asn = "AS0",
                            countryCode = "IT",
                            geoIpdb = null,
                            networkType = NetworkType.Wifi,
                        ),
                        TaskEvent.MeasurementStart(0, "https://example.com"),
                    )
                },
                getOrCreateUrl = { error("Should not be called") },
                storeMeasurement = { MeasurementModel.Id(1) },
                storeNetwork = { NetworkModel.Id(1) },
                getResultByIdAndUpdate = { _, update ->
                    val result = ResultModel(
                        runId = RunModel.Id("1"),
                        descriptorName = "name",
                        descriptorKey = null,
                        taskOrigin = TaskOrigin.OoniRun,
                    )
                    failureMessage = update(result).failureMessage
                },
                setCurrentTestState = {},
                writeFile = { _, _ -> },
                deleteFiles = { },
                json = Dependencies.buildJson(),
                getPreferenceValueByKey = { flowOf(true) },
                submitMeasurement = { null },
                noInternetWatcher = NoInternetWatcher { NetworkType.Wifi },
                cancelRun = {},
                reportTestRunError = {},
                spec = spec,
            )

            subject.invoke()

            assertEquals("ASN is 0", failureMessage)
        }

    @Test
    fun cancelsRunAfterConsecutiveMeasurementsWithoutInternet() =
        runTest {
            var cancelCount = 0
            val errors = mutableListOf<TestRunError>()

            buildSubject(
                measurementsCount = 10,
                noInternetWatcher = NoInternetWatcher { NetworkType.NoInternet },
                cancelRun = { cancelCount++ },
                reportTestRunError = { errors += it },
            ).invoke()

            assertEquals(1, cancelCount)
            assertEquals(listOf<TestRunError>(TestRunError.NoInternet), errors)
        }

    @Test
    fun doesNotCancelRunWhenInternetComesBack() =
        runTest {
            var cancelCount = 0
            var checks = 0

            buildSubject(
                measurementsCount = 10,
                // Internet only drops for fewer measurements than the threshold, then comes back
                noInternetWatcher = NoInternetWatcher {
                    checks++
                    if (checks % NoInternetWatcher.NO_INTERNET_CANCEL_THRESHOLD == 0) {
                        NetworkType.Wifi
                    } else {
                        NetworkType.NoInternet
                    }
                },
                cancelRun = { cancelCount++ },
                reportTestRunError = {},
            ).invoke()

            assertEquals(0, cancelCount)
            assertTrue(checks >= 10)
        }

    @Test
    fun skipsSubmitMeasurementWhenCancelledDueToNoInternet() =
        runTest {
            var submitCount = 0
            var cancelCount = 0

            buildSubject(
                measurementsCount = 5,
                noInternetWatcher = NoInternetWatcher { NetworkType.NoInternet },
                cancelRun = { cancelCount++ },
                reportTestRunError = {},
                uploadResults = true,
                submitMeasurement = {
                    submitCount++
                    null
                },
            ).invoke()

            assertEquals(2, submitCount)
            assertEquals(1, cancelCount)
        }

    @Test
    fun cancelsRunAfterConsecutiveMeasurementsWithoutInternetAcrossMultipleTests() =
        runTest {
            var cancelCount = 0
            val errors = mutableListOf<TestRunError>()
            // Shared by the whole run, as RunDescriptors does
            val noInternetWatcher = NoInternetWatcher { NetworkType.NoInternet }

            // Test 1: 2 measurements without Internet
            buildSubject(
                measurementsCount = 2,
                noInternetWatcher = noInternetWatcher,
                cancelRun = { cancelCount++ },
                reportTestRunError = { errors += it },
            ).invoke()

            assertEquals(0, cancelCount)
            assertTrue(errors.isEmpty())

            // Test 2: first measurement is the 3rd consecutive check without Internet
            buildSubject(
                measurementsCount = 2,
                noInternetWatcher = noInternetWatcher,
                cancelRun = { cancelCount++ },
                reportTestRunError = { errors += it },
            ).invoke()

            assertEquals(1, cancelCount)
            assertEquals(listOf<TestRunError>(TestRunError.NoInternet), errors)
        }

    @Test
    fun cancelsRunWhenInternetDropsAfterFirstMeasurementSucceeded() =
        runTest {
            var cancelCount = 0
            var checks = 0

            buildSubject(
                measurementsCount = 10,
                // Online for the first measurement, offline afterwards
                noInternetWatcher = NoInternetWatcher {
                    checks++
                    if (checks == 1) NetworkType.Wifi else NetworkType.NoInternet
                },
                cancelRun = { cancelCount++ },
                reportTestRunError = {},
            ).invoke()

            assertEquals(1, cancelCount)
            // 1 online check + threshold offline checks, then later measurements stop checking
            assertEquals(1 + NoInternetWatcher.NO_INTERNET_CANCEL_THRESHOLD, checks)
        }

    @Test
    fun testsWithoutMeasurementsCountTowardsNoInternetThreshold() =
        runTest {
            var cancelCount = 0
            val errors = mutableListOf<TestRunError>()
            val noInternetWatcher = NoInternetWatcher { NetworkType.NoInternet }

            // Offline, the engine fails at startup and emits no measurement
            repeat(NoInternetWatcher.NO_INTERNET_CANCEL_THRESHOLD) {
                buildSubject(
                    measurementsCount = 0,
                    noInternetWatcher = noInternetWatcher,
                    cancelRun = { cancelCount++ },
                    reportTestRunError = { errors += it },
                ).invoke()
            }

            assertEquals(1, cancelCount)
            assertEquals(listOf<TestRunError>(TestRunError.NoInternet), errors)
        }

    @Test
    fun testWithMeasurementsChecksInternetOnlyPerMeasurement() =
        runTest {
            var checks = 0
            val noInternetWatcher = NoInternetWatcher {
                checks++
                NetworkType.Wifi
            }

            buildSubject(
                measurementsCount = 2,
                noInternetWatcher = noInternetWatcher,
                cancelRun = {},
                reportTestRunError = {},
            ).invoke()

            assertEquals(2, checks)
        }

    private fun buildSubject(
        measurementsCount: Int,
        noInternetWatcher: NoInternetWatcher,
        cancelRun: () -> Unit,
        reportTestRunError: (TestRunError) -> Unit,
        uploadResults: Boolean = false,
        submitMeasurement: (suspend (MeasurementModel) -> MeasurementModel?)? = null,
    ): RunNetTest {
        val descriptor = DescriptorFactory.buildDescriptorWithInstalled(
            netTests = listOf(NetTest(TestType.WebConnectivity)),
        )
        val spec = RunNetTest.Specification(
            descriptor = descriptor,
            descriptorIndex = 0,
            netTest = descriptor.netTests.first(),
            taskOrigin = TaskOrigin.OoniRun,
            isRerun = false,
            resultId = ResultModel.Id(1),
            testIndex = 0,
            testTotal = 1,
        )
        val events = (0 until measurementsCount).flatMap { index ->
            listOf(
                TaskEvent.MeasurementStart(index, null),
                TaskEvent.MeasurementDone(index),
            )
        }
        return RunNetTest(
            startTest = { _, _, _ -> flowOf(*events.toTypedArray()) },
            getOrCreateUrl = { error("Should not be called") },
            storeMeasurement = { MeasurementModel.Id(1) },
            storeNetwork = { NetworkModel.Id(1) },
            getResultByIdAndUpdate = { _, _ -> },
            setCurrentTestState = {},
            writeFile = { _, _ -> },
            deleteFiles = { },
            json = Dependencies.buildJson(),
            getPreferenceValueByKey = { flowOf(uploadResults) },
            submitMeasurement = submitMeasurement ?: { null },
            noInternetWatcher = noInternetWatcher,
            cancelRun = cancelRun,
            reportTestRunError = reportTestRunError,
            spec = spec,
        )
    }
}
