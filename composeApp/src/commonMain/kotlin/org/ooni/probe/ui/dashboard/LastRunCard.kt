package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import ooniprobe.composeapp.generated.resources.Common_Dismiss
import ooniprobe.composeapp.generated.resources.Dashboard_LastResults
import ooniprobe.composeapp.generated.resources.Dashboard_LastResults_SeeResults
import ooniprobe.composeapp.generated.resources.Measurements_Failed_Count
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Websites_Blocked
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Websites_Tested
import ooniprobe.composeapp.generated.resources.ic_history
import ooniprobe.composeapp.generated.resources.ic_measurement_anomaly
import ooniprobe.composeapp.generated.resources.ic_measurement_failed
import ooniprobe.composeapp.generated.resources.ic_world
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.RunModel
import org.ooni.probe.data.models.RunSummary
import org.ooni.probe.shared.now
import org.ooni.probe.ui.shared.relativeDateTime
import org.ooni.probe.ui.theme.AppTheme
import org.ooni.probe.ui.theme.LocalCustomColors
import org.ooni.probe.ui.theme.cardTitle

@Composable
fun LastRunCard(
    run: RunSummary,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    DashboardCard(
        title = {
            Text(
                stringResource(Res.string.Dashboard_LastResults),
                style = MaterialTheme.typography.cardTitle,
            )
            Text(
                run.run.startTime.relativeDateTime(),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
            )
        },
        content = {
            FlowRow {
                ResultChip(
                    text = pluralStringResource(
                        Res.plurals.TestResults_Overview_Websites_Tested,
                        run.measurementCounts.done.toInt(),
                        run.measurementCounts.done,
                    ),
                    icon = Res.drawable.ic_world,
                    modifier = Modifier.padding(end = 2.dp),
                )

                if (run.measurementCounts.anomaly > 0) {
                    ResultChip(
                        text = pluralStringResource(
                            Res.plurals.TestResults_Overview_Websites_Blocked,
                            run.measurementCounts.anomaly.toInt(),
                            run.measurementCounts.anomaly,
                        ),
                        icon = Res.drawable.ic_measurement_anomaly,
                        iconTint = LocalCustomColors.current.logWarn,
                        modifier = Modifier.padding(end = 2.dp),
                    )
                }

                if (run.measurementCounts.failed > 0) {
                    ResultChip(
                        text = pluralStringResource(
                            Res.plurals.Measurements_Failed_Count,
                            run.measurementCounts.failed.toInt(),
                            run.measurementCounts.failed,
                        ),
                        icon = Res.drawable.ic_measurement_failed,
                        iconTint = MaterialTheme.colorScheme.error,
                        modifier = Modifier,
                    )
                }
            }
        },
        startActions = {
            TextButton(onClick = { onEvent(DashboardViewModel.Event.DismissResultsClicked) }) {
                Text(
                    stringResource(Res.string.Common_Dismiss),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                )
            }
        },
        endActions = {
            TextButton(onClick = { onEvent(DashboardViewModel.Event.SeeResultsClicked) }) {
                Text(stringResource(Res.string.Dashboard_LastResults_SeeResults))
            }
        },
        icon = painterResource(Res.drawable.ic_history),
    )
}

@Composable
fun ResultChip(
    text: String,
    icon: DrawableResource,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text = text) },
        leadingIcon = {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = iconTint ?: LocalContentColor.current,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            disabledLabelColor = LocalContentColor.current,
            disabledLeadingIconContentColor = LocalContentColor.current,
        ),
        border = AssistChipDefaults.assistChipBorder(true),
        modifier = modifier,
    )
}

@Preview
@Composable
fun LastRunCardPreview() {
    AppTheme {
        LastRunCard(
            run = RunSummary(
                run = RunModel(
                    id = RunModel.Id("1"),
                    network = null,
                    startTime = LocalDateTime.now(),
                    taskOrigin = TaskOrigin.AutoRun,
                ),
                results = emptyList(),
            ),
            onEvent = {},
        )
    }
}
