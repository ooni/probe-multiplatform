package org.ooni.probe.data.models

import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_Gbps
import ooniprobe.composeapp.generated.resources.TestResults_Kbps
import ooniprobe.composeapp.generated.resources.TestResults_Mbps
import ooniprobe.composeapp.generated.resources.TestResults_NotAvailable
import ooniprobe.composeapp.generated.resources.r1080p
import ooniprobe.composeapp.generated.resources.r1080p_ext
import ooniprobe.composeapp.generated.resources.r1440p
import ooniprobe.composeapp.generated.resources.r1440p_ext
import ooniprobe.composeapp.generated.resources.r2160p
import ooniprobe.composeapp.generated.resources.r2160p_ext
import ooniprobe.composeapp.generated.resources.r240p
import ooniprobe.composeapp.generated.resources.r360p
import ooniprobe.composeapp.generated.resources.r480p
import ooniprobe.composeapp.generated.resources.r720p
import ooniprobe.composeapp.generated.resources.r720p_ext
import org.jetbrains.compose.resources.StringResource
import org.ooni.engine.models.TestKeys
import org.ooni.engine.models.TestType
import org.ooni.probe.ui.shared.format

data class TestKeysWithResultId(
    val id: MeasurementModel.Id,
    val testName: String?,
    val testKeys: TestKeys?,
    val resultId: ResultModel.Id,
    val descriptorName: String?,
    val descriptorRunId: Descriptor.Id?,
)

fun List<TestKeysWithResultId>.videoQuality() =
    this.firstOrNull { TestType.Dash.name == it.testName }?.testKeys?.getVideoQuality(extended = false)

fun List<TestKeysWithResultId>.uploadSpeed() =
    this.firstOrNull { TestType.Ndt.name == it.testName }?.testKeys?.let { testKey ->
        return@let testKey.summary?.upload?.let {
            val upload = setFractionalDigits(getScaledValue(it))
            val unit = getUnit(it)
            upload to unit
        }
    }

fun List<TestKeysWithResultId>.downloadSpeed() =
    this.firstOrNull { TestType.Ndt.name == it.testName }?.testKeys?.let { testKey ->
        return@let testKey.summary?.download?.let {
            val download = setFractionalDigits(getScaledValue(it))
            val unit = getUnit(it)
            download to unit
        }
    }

fun List<TestKeysWithResultId>.ping() =
    this
        .firstOrNull { TestType.Ndt.name == it.testName }
        ?.testKeys
        ?.summary
        ?.ping
        ?.format(1)

fun TestKeys.getVideoQuality(extended: Boolean): StringResource {
    return simple?.medianBitrate?.let {
        return minimumBitrateForVideo(it, extended)
    } ?: Res.string.TestResults_NotAvailable
}

private fun minimumBitrateForVideo(
    videoQuality: Double,
    extended: Boolean,
): StringResource =
    if (videoQuality < 600) {
        Res.string.r240p
    } else if (videoQuality < 1000) {
        Res.string.r360p
    } else if (videoQuality < 2500) {
        Res.string.r480p
    } else if (videoQuality < 5000) {
        if (extended) {
            Res.string.r720p_ext
        } else {
            Res.string.r720p
        }
    } else if (videoQuality < 8000) {
        if (extended) {
            Res.string.r1080p_ext
        } else {
            Res.string.r1080p
        }
    } else if (videoQuality < 16000) {
        if (extended) {
            Res.string.r1440p_ext
        } else {
            Res.string.r1440p
        }
    } else if (extended) {
        Res.string.r2160p_ext
    } else {
        Res.string.r2160p
    }

fun getScaledValue(value: Double): Double =
    if (value < 1000) {
        value
    } else if (value < 1000 * 1000) {
        value / 1000
    } else {
        value / 1000 * 1000
    }

fun setFractionalDigits(value: Double): String = if (value < 10) value.format(1) else value.format(2)

fun getUnit(value: Double): StringResource {
    // We assume there is no Tbit/s (for now!)
    return if (value < 1000) {
        Res.string.TestResults_Kbps
    } else if (value < 1000 * 1000) {
        Res.string.TestResults_Mbps
    } else {
        Res.string.TestResults_Gbps
    }
}
