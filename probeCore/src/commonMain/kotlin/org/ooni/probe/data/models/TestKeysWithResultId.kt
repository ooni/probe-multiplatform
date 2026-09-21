package org.ooni.probe.data.models

import org.ooni.engine.models.TestKeys
import org.ooni.engine.models.TestType
import org.ooni.probe.shared.format

data class TestKeysWithResultId(
    val id: MeasurementModel.Id,
    val testName: String?,
    val testKeys: TestKeys?,
    val resultId: ResultModel.Id,
    val descriptorName: String?,
    val descriptorRunId: Descriptor.Id?,
)

fun List<TestKeysWithResultId>.uploadSpeed() =
    firstOrNull { TestType.Ndt.name == it.testName }
        ?.testKeys
        ?.summary
        ?.upload
        ?.let(::ScaledValue)

fun List<TestKeysWithResultId>.downloadSpeed() =
    firstOrNull { TestType.Ndt.name == it.testName }
        ?.testKeys
        ?.summary
        ?.download
        ?.let(::ScaledValue)

fun List<TestKeysWithResultId>.ping() =
    firstOrNull { TestType.Ndt.name == it.testName }
        ?.testKeys
        ?.summary
        ?.ping
        ?.format(1)
