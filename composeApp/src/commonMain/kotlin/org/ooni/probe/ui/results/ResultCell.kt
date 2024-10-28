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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Modal_UploadFailed_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Snackbar_ResultsNotUploaded_Text
import ooniprobe.composeapp.generated.resources.TestResults_UnknownASN
import ooniprobe.composeapp.generated.resources.ic_cloud_off
import ooniprobe.composeapp.generated.resources.measurements_count
import ooniprobe.composeapp.generated.resources.task_origin_auto_run
import ooniprobe.composeapp.generated.resources.task_origin_manual
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.ui.dashboard.TestDescriptorLabel
import org.ooni.probe.ui.shared.relativeDateTime

@Composable
fun ResultCell(
    item: ResultListItem,
    onResultClick: () -> Unit,
) {
    val hasError = item.result.isDone && item.measurementsCount == 0L

    Surface(
        color = if (item.result.isViewed || hasError) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
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

                if (hasError) {
                    Text(
                        item.result.failureMessage.orEmpty().lines().first(),
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
                    item.result.startTime.relativeDateTime(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(0.34f),
            ) {
                if (!item.result.isDone) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(bottom = 4.dp)
                            .size(24.dp),
                    )
                }
                Text(
                    stringResource(
                        when (item.result.taskOrigin) {
                            TaskOrigin.AutoRun -> Res.string.task_origin_auto_run
                            TaskOrigin.OoniRun -> Res.string.task_origin_manual
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                if (!hasError) {
                    Text(
                        pluralStringResource(
                            Res.plurals.measurements_count,
                            item.measurementsCount.toInt(),
                            item.measurementsCount,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
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
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp),
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
