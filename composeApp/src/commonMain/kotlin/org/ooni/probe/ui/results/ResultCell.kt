package org.ooni.probe.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Measurements_Count
import ooniprobe.composeapp.generated.resources.Measurements_Failed_Count
import ooniprobe.composeapp.generated.resources.Modal_UploadFailed_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Snackbar_ResultsNotUploaded_Text
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Websites_Blocked
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Websites_Tested
import ooniprobe.composeapp.generated.resources.ic_check_circle
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
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.SummaryType
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.data.models.downloadSpeed
import org.ooni.probe.data.models.uploadSpeed
import org.ooni.probe.data.models.videoQuality
import org.ooni.probe.ui.dashboard.TestDescriptorLabel
import org.ooni.probe.ui.theme.LocalCustomColors

@Composable
fun ResultCell(
    item: ResultListItem,
    onResultClick: () -> Unit,
    isSelected: Boolean = false,
    onSelectChange: ((Boolean) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val hasError = item.result.isDone && item.measurementCounts.done == 0L

    Surface(
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else if (item.result.isViewed || hasError) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (isSelected) {
                            onSelectChange?.invoke(false)
                        } else {
                            onResultClick()
                        }
                    },
                    onLongClick = onLongClick,
                ).padding(start = 16.dp, end = 4.dp)
                .padding(vertical = 12.dp)
                .testTag(item.descriptor.key),
        ) {
            Column(
                modifier = Modifier.padding(end = 4.dp).weight(0.6f),
            ) {
                Box {
                    TestDescriptorLabel(
                        item.descriptor,
                        modifier = if (hasError) Modifier.alpha(0.5f) else Modifier,
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = Color.White,
                                    shape = MaterialTheme.shapes.small,
                                ),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_check_circle),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center),
                            )
                        }
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Top),
                modifier = Modifier.weight(0.4f).padding(end = 4.dp),
            ) {
                if (!hasError) {
                    ResultCounts(item)
                }
            }
        }
    }
}

@Composable
private fun ResultCounts(item: ResultListItem) {
    val summaryType = item.descriptor.summaryType
    val counts = item.measurementCounts

    if (counts.failed > 0) {
        ResultCountItem(
            icon = Res.drawable.ic_measurement_failed,
            text = pluralStringResource(
                Res.plurals.Measurements_Failed_Count,
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
                text = pluralStringResource(
                    Res.plurals.Measurements_Count,
                    counts.done.toInt(),
                    counts.done,
                ),
            )
        }

        SummaryType.Anomaly -> {
            if (counts.anomaly > 0) {
                ResultCountItem(
                    icon = Res.drawable.ic_measurement_anomaly,
                    text = pluralStringResource(
                        Res.plurals.TestResults_Overview_Websites_Blocked,
                        counts.anomaly.toInt(),
                        counts.anomaly,
                    ),
                    color = LocalCustomColors.current.logWarn,
                )
            }
            ResultCountItem(
                icon = Res.drawable.ic_world,
                text = pluralStringResource(
                    Res.plurals.TestResults_Overview_Websites_Tested,
                    counts.done.toInt(),
                    counts.done,
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

    if (!item.allMeasurementsUploaded) {
        ResultCountItem(
            icon = Res.drawable.ic_cloud_off,
            text = stringResource(
                if (item.anyMeasurementUploadFailed) {
                    Res.string.Modal_UploadFailed_Title
                } else {
                    Res.string.Snackbar_ResultsNotUploaded_Text
                },
            ).lowercase(),
        )
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
        modifier = Modifier.metricChip(color),
    ) {
        Icon(
            painter = painterResource(icon),
            tint = color.copy(alpha = 0.66f),
            contentDescription = null,
            modifier = Modifier.padding(end = 2.dp).size(12.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
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
    text?.let {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.metricChip(color),
        ) {
            Icon(
                painter = painterResource(icon),
                tint = color.copy(alpha = 0.66f),
                contentDescription = null,
                modifier = Modifier.padding(end = 2.dp).size(12.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}

@Composable
private fun Modifier.metricChip(color: Color = LocalContentColor.current) =
    this
        .border(
            width = 0.5.dp,
            color = color.copy(alpha = 0.5f),
            shape = RoundedCornerShape(4.dp),
        ).padding(horizontal = 4.dp, vertical = 2.dp)
