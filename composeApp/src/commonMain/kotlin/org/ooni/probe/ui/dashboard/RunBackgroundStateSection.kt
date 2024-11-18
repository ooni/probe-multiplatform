package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Dashboard_Overview_LatestTest
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_RunFinished
import ooniprobe.composeapp.generated.resources.Dashboard_Running_EstimatedTimeLeft
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Running
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Stopping_Notice
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Stopping_Title
import ooniprobe.composeapp.generated.resources.Modal_ResultsNotUploaded_Uploading
import ooniprobe.composeapp.generated.resources.OONIRun_Run
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_timer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.domain.UploadMissingMeasurements
import org.ooni.probe.ui.shared.relativeDateTime
import org.ooni.probe.ui.shared.shortFormat
import org.ooni.probe.ui.theme.customColors

@Composable
fun RunBackgroundStateSection(
    state: RunBackgroundState,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    when (state) {
        is RunBackgroundState.Idle -> {
            OutlinedButton(
                onClick = { onEvent(DashboardViewModel.Event.RunTestsClick) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                    brush = SolidColor(MaterialTheme.colorScheme.primary),
                ),
                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp),
            ) {
                Text(
                    stringResource(Res.string.OONIRun_Run),
                    style = MaterialTheme.typography.titleLarge,
                )
                Icon(
                    painterResource(Res.drawable.ic_timer),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            state.lastTestAt?.let { lastTestAt ->
                Text(
                    text = stringResource(Res.string.Dashboard_Overview_LatestTest) + " " + lastTestAt.relativeDateTime(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (state.justFinishedTest) {
                Button(
                    onClick = { onEvent(DashboardViewModel.Event.SeeResultsClick) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.customColors.success,
                        contentColor = MaterialTheme.customColors.onSuccess,
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(stringResource(Res.string.Dashboard_RunV2_RunFinished))
                }
            }
        }

        is RunBackgroundState.UploadingMissingResults -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 32.dp, bottom = 8.dp),
            ) {
                val progressColor = MaterialTheme.colorScheme.primary
                val progressTrackColor = MaterialTheme.colorScheme.onBackground
                val progressModifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(8.dp)

                when (val uploadState = state.state) {
                    is UploadMissingMeasurements.State.Uploading -> {
                        val progress = uploadState.uploaded + uploadState.failedToUpload + 1
                        Text(
                            text = stringResource(
                                Res.string.Modal_ResultsNotUploaded_Uploading,
                                "$progress/${uploadState.total}",
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        LinearProgressIndicator(
                            color = progressColor,
                            trackColor = progressTrackColor,
                            modifier = progressModifier,
                        )
                    }
                    UploadMissingMeasurements.State.Starting,
                    is UploadMissingMeasurements.State.Finished,
                    -> {
                        LinearProgressIndicator(
                            color = progressColor,
                            trackColor = progressTrackColor,
                            modifier = progressModifier,
                        )
                    }
                }
            }
        }

        is RunBackgroundState.RunningTests -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onEvent(DashboardViewModel.Event.RunningTestClick) }
                    .padding(horizontal = 16.dp)
                    .padding(top = 32.dp, bottom = 8.dp),
            ) {
                Row {
                    Text(
                        text = stringResource(Res.string.Dashboard_Running_Running),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    state.testType?.let { testType ->
                        testType.iconRes?.let {
                            Icon(
                                painterResource(it),
                                contentDescription = null,
                                modifier = Modifier.padding(horizontal = 4.dp).size(24.dp),
                            )
                        }
                        Text(
                            text = testType.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                state.progress.let { progress ->
                    val color = MaterialTheme.colorScheme.primary
                    val trackColor = MaterialTheme.colorScheme.onBackground
                    val modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(8.dp)

                    if (progress == 0.0) {
                        LinearProgressIndicator(
                            color = color,
                            trackColor = trackColor,
                            modifier = modifier,
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { progress.toFloat() },
                            color = color,
                            trackColor = trackColor,
                            modifier = modifier,
                        )
                    }
                }

                state.estimatedTimeLeft?.let { timeLeft ->
                    Row {
                        Text(
                            text = stringResource(Res.string.Dashboard_Running_EstimatedTimeLeft),
                        )
                        Text(
                            text = " " + timeLeft.shortFormat(),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        RunBackgroundState.Stopping -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 32.dp, bottom = 8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.Dashboard_Running_Stopping_Title),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(Res.string.Dashboard_Running_Stopping_Notice),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
