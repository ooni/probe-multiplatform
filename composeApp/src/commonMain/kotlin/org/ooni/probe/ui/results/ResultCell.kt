package org.ooni.probe.ui.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Measurements_Count
import ooniprobe.composeapp.generated.resources.Modal_UploadFailed_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Snackbar_ResultsNotUploaded_Text
import ooniprobe.composeapp.generated.resources.TaskOrigin_AutoRun
import ooniprobe.composeapp.generated.resources.TaskOrigin_Manual
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Websites_Blocked
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Websites_Tested
import ooniprobe.composeapp.generated.resources.TestResults_UnknownASN
import ooniprobe.composeapp.generated.resources.ic_cloud_off
import ooniprobe.composeapp.generated.resources.ic_download
import ooniprobe.composeapp.generated.resources.ic_history
import ooniprobe.composeapp.generated.resources.ic_measurement_anomaly
import ooniprobe.composeapp.generated.resources.ic_measurement_failed
import ooniprobe.composeapp.generated.resources.ic_upload
import ooniprobe.composeapp.generated.resources.ic_world
import ooniprobe.composeapp.generated.resources.twoParam
import ooniprobe.composeapp.generated.resources.video_quality
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.SummaryType
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.data.models.downloadSpeed
import org.ooni.probe.data.models.uploadSpeed
import org.ooni.probe.data.models.videoQuality
import org.ooni.probe.shared.pluralStringResourceItem
import org.ooni.probe.ui.dashboard.TestDescriptorLabel
import org.ooni.probe.ui.shared.relativeDateTime
import org.ooni.probe.ui.theme.LocalCustomColors

@Composable
fun ResultCell(
    item: ResultListItem,
    onResultClick: () -> Unit,
) {
    val hasError = item.result.isDone && item.measurementCounts.done == 0L

    Surface(
        color = if (item.result.isViewed || hasError) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .run { if (!hasError) clickable { onResultClick() } else this }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier.weight(0.66f),
            ) {
                TestDescriptorLabel(
                    item.descriptor,
                    modifier = if (hasError) Modifier.alpha(0.5f) else Modifier,
                )

                item.network?.networkName?.let { networkName ->
                    Text(
                        networkName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (hasError && !item.result.failureMessage.isNullOrEmpty()) {
                    Text(
                        item.result.failureMessage.lines().first(),
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    val asn = item.network?.asn ?: stringResource(Res.string.TestResults_UnknownASN)
                    Text(
                        "($asn)",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    item.result.startTime.relativeDateTime() + " – " + item.sourceText,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(start = 8.dp, top = 24.dp).weight(0.35f),
            ) {
                if (!item.result.isDone) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(bottom = 4.dp).size(24.dp),
                    )
                }
                if (!hasError) {
                    ResultCounts(item)
                }
                if (!item.allMeasurementsUploaded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_cloud_off),
                            contentDescription = null,
                            tint = if (item.anyMeasurementUploadFailed) {
                                MaterialTheme.colorScheme.error
                            } else {
                                LocalContentColor.current
                            },
                            modifier = Modifier.size(20.dp).padding(end = 4.dp),
                        )
                        Text(
                            stringResource(
                                if (item.anyMeasurementUploadFailed) {
                                    Res.string.Modal_UploadFailed_Title
                                } else {
                                    Res.string.Snackbar_ResultsNotUploaded_Text
                                },
                            ).lowercase(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultCounts(item: ResultListItem) {
    val summaryType = item.descriptor.summaryType
    val counts = item.measurementCounts

    Column {
        if (counts.failed > 0) {
            ResultCountItem(
                icon = Res.drawable.ic_measurement_failed,
                text = pluralStringResourceItem(
                    Res.plurals.TestResults_Overview_Websites_Blocked,
                    counts.failed.toInt(),
                    counts.failed,
                ),
                color = MaterialTheme.colorScheme.error,
            )
        }

        when (summaryType) {
            SummaryType.Simple -> {
                ResultCountItem(
                    icon = Res.drawable.ic_history,
                    text = pluralStringResourceItem(
                        Res.plurals.Measurements_Count,
                        counts.done.toInt(),
                        counts.done,
                    ),
                )
            }
            SummaryType.Anomaly -> {
                ResultCountItem(
                    icon = Res.drawable.ic_measurement_anomaly,
                    text = pluralStringResourceItem(
                        Res.plurals.TestResults_Overview_Websites_Blocked,
                        counts.anomaly.toInt(),
                        counts.anomaly,
                    ),
                    color = if (counts.anomaly > 0) {
                        LocalCustomColors.current.logWarn
                    } else {
                        LocalContentColor.current
                    },
                )
                ResultCountItem(
                    icon = Res.drawable.ic_world,
                    text = pluralStringResourceItem(
                        Res.plurals.TestResults_Overview_Websites_Tested,
                        counts.tested.toInt(),
                        counts.tested,
                    ),
                )
            }
            SummaryType.Performance -> {
                item.testKeys?.downloadSpeed()?.let { (download, unit) ->

                    PerformanceMetric(
                        icon = Res.drawable.ic_download,
                        text = stringResource(Res.string.twoParam, download, stringResource(unit)),
                    )
                }
                item.testKeys?.uploadSpeed()?.let { (upload, unit) ->

                    PerformanceMetric(
                        icon = Res.drawable.ic_upload,
                        text = stringResource(Res.string.twoParam, upload, stringResource(unit)),
                    )
                }

                PerformanceMetric(
                    icon = Res.drawable.video_quality,
                    text = item.testKeys?.videoQuality()?.let {
                        stringResource(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun ResultCountItem(
    icon: DrawableResource,
    text: String,
    color: Color = LocalContentColor.current,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 2.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            tint = color.copy(alpha = 0.66f),
            contentDescription = null,
            modifier = Modifier.padding(end = 2.dp).size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

@Composable
private fun PerformanceMetric(
    icon: DrawableResource,
    text: String?,
    color: Color = LocalContentColor.current,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 2.dp),
    ) {
        text?.let {
            Icon(
                painter = painterResource(icon),
                tint = color.copy(alpha = 0.66f),
                contentDescription = null,
                modifier = Modifier.padding(end = 2.dp).size(16.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        }
    }
}

private val ResultListItem.sourceText
    @Composable
    get() = stringResource(
        when (result.taskOrigin) {
            TaskOrigin.AutoRun -> Res.string.TaskOrigin_AutoRun
            TaskOrigin.OoniRun -> Res.string.TaskOrigin_Manual
        },
    )
