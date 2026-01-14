package org.ooni.probe.ui.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Measurements_Count
import ooniprobe.composeapp.generated.resources.Measurements_Failed
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TaskOrigin_AutoRun
import ooniprobe.composeapp.generated.resources.TaskOrigin_Manual
import ooniprobe.composeapp.generated.resources.TestResults_NotAvailable
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Performance_Hero_Download
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Performance_Hero_Upload
import ooniprobe.composeapp.generated.resources.TestResults_UnknownASN
import ooniprobe.composeapp.generated.resources.ic_download
import ooniprobe.composeapp.generated.resources.ic_history
import ooniprobe.composeapp.generated.resources.ic_keyboard_arrow_down
import ooniprobe.composeapp.generated.resources.ic_keyboard_arrow_up
import ooniprobe.composeapp.generated.resources.ic_measurement_failed
import ooniprobe.composeapp.generated.resources.ic_upload
import ooniprobe.composeapp.generated.resources.ooni_empty_state
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.ui.shared.relativeDateTime

@Composable
fun RunCell(
    item: RunListItem,
    onClick: () -> Unit,
) {
    val network = item.run.network

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(start = 16.dp)
            .padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(0.66f)) {
            val asn = if (network?.isValid() == false) {
                stringResource(Res.string.TestResults_NotAvailable)
            } else {
                network?.asn ?: stringResource(Res.string.TestResults_UnknownASN)
            }
            val networkText = network?.networkName?.let { "$it " } + "($asn)"
            Text(
                networkText,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                item.run.startTime.relativeDateTime() + " – " + item.sourceText,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        if (!item.isExpanded) {
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(0.33f)
            ) {
                FlowRow(
                    itemVerticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    item.results.take(MAX_DESCRIPTOR_ICONS).forEach { result ->
                        val descriptor = result.item.descriptor
                        Icon(
                            painter = painterResource(
                                descriptor.icon ?: Res.drawable.ooni_empty_state
                            ),
                            contentDescription = null,
                            tint = descriptor.color ?: Color.Unspecified,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(16.dp),
                        )
                    }
                    if (item.results.size > MAX_DESCRIPTOR_ICONS) {
                        Text(
                            "+" + (item.results.size - MAX_DESCRIPTOR_ICONS),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                val counts = item.measurementCounts
                if (counts.failed > 0) {
                    ResultCountItem(
                        icon = Res.drawable.ic_measurement_failed,
                        text = pluralStringResource(
                            Res.plurals.Measurements_Failed,
                            counts.failed.toInt(),
                            counts.failed,
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                ResultCountItem(
                    icon = Res.drawable.ic_history,
                    text = pluralStringResource(
                        Res.plurals.Measurements_Count,
                        counts.tested.toInt(),
                        counts.tested,
                    ),
                )
            }
        }
        if (!item.isDone) {
            CircularProgressIndicator(
                modifier = Modifier.padding(start = 8.dp).size(24.dp),
            )
        }
        Icon(
            painterResource(
                if (item.isExpanded) {
                    Res.drawable.ic_keyboard_arrow_up
                } else {
                    Res.drawable.ic_keyboard_arrow_down
                }
            ),
            contentDescription = stringResource(
                if (item.isExpanded) {
                    Res.string.TestResults_Summary_Performance_Hero_Upload
                } else {
                    Res.string.TestResults_Summary_Performance_Hero_Download
                }
            ),
            Modifier.padding(horizontal = 8.dp)
        )
    }
}

private val RunListItem.sourceText
    @Composable
    get() = stringResource(
        when (run.taskOrigin) {
            TaskOrigin.AutoRun -> Res.string.TaskOrigin_AutoRun
            TaskOrigin.OoniRun -> Res.string.TaskOrigin_Manual
        },
    )

private const val MAX_DESCRIPTOR_ICONS = 5
