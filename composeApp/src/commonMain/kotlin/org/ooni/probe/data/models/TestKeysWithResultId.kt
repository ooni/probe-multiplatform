package org.ooni.probe.data.models

import ooniprobe.composeapp.generated.resources.Res
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
import org.ooni.probe.shared.format

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

fun TestKeys.getVideoQuality(extended: Boolean): StringResource =
    simple
        ?.medianBitrate
        ?.let { minimumBitrateForVideo(it, extended) }
        ?: Res.string.TestResults_NotAvailable

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
