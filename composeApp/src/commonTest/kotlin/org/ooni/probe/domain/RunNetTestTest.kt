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
import org.ooni.probe.di.Dependencies
import org.ooni.testing.factories.DescriptorFactory
import kotlin.test.Test
import kotlin.test.assertEquals

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
                spec = spec,
            )

            subject.invoke()

            assertEquals("ASN is 0", failureMessage)
        }
}
