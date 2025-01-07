package org.ooni.probe.data.models

import androidx.compose.runtime.Composable
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.twoParam
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TestType

data class ResultListItem(
    val result: ResultModel,
    val descriptor: Descriptor,
    val network: NetworkModel?,
    val measurementCounts: MeasurementCounts,
    val allMeasurementsUploaded: Boolean,
    val anyMeasurementUploadFailed: Boolean,
    val testKeys: List<TestKeysWithResultId>?,
) {
    val idOrThrow
        get() = result.idOrThrow

    val videoQuality
        get() = testKeys?.firstOrNull { TestType.Dash.name == it.testName }?.testKeys?.getVideoQuality(extended = false)

    val upload
        @Composable
        get() = testKeys?.firstOrNull { TestType.Ndt.name == it.testName }?.testKeys?.let { testKey ->
            return@let testKey.summary?.upload?.let {
                val upload = setFractionalDigits(getScaledValue(it))
                val unit = getUnit(it)
                stringResource(Res.string.twoParam, upload, stringResource(unit))
            }
        }

    val download
        @Composable
        get() = testKeys?.firstOrNull { TestType.Ndt.name == it.testName }?.testKeys?.let { testKey ->
            return@let testKey.summary?.download?.let {
                val download = setFractionalDigits(getScaledValue(it))
                val unit = getUnit(it)
                stringResource(Res.string.twoParam, download, stringResource(unit))
            }
        }
}
