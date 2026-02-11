package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import ooniprobe.composeapp.generated.resources.Dashboard_Running_EstimatedTimeLeft
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Running
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Stopping_Notice
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Stopping_Title
import ooniprobe.composeapp.generated.resources.OONIRun_Run
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Results_UploadingMissing
import ooniprobe.composeapp.generated.resources.ic_timer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.domain.UploadMissingMeasurements
import org.ooni.probe.ui.shared.format
import org.ooni.probe.ui.theme.AppTheme

@Composable
fun RunBackgroundStateSection(
    state: RunBackgroundState,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    when (state) {
        is RunBackgroundState.Idle -> Idle(state, onEvent)
        is RunBackgroundState.UploadingMissingResults -> UploadingMissingResults(state)
        is RunBackgroundState.RunningTests -> RunningTests(state, onEvent)
        RunBackgroundState.Stopping -> Stopping()
    }
}

@Composable
private fun Idle(
    state: RunBackgroundState.Idle,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    OutlinedButton(
        onClick = { onEvent(DashboardViewModel.Event.RunTestsClicked) },
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
}

@Composable
private fun UploadingMissingResults(state: RunBackgroundState.UploadingMissingResults) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 32.dp, bottom = 8.dp),
    ) {
        val progressColor = MaterialTheme.colorScheme.primary
        val progressTrackColor = MaterialTheme.colorScheme.onBackground
        val progressModifier =
            Modifier.fillMaxWidth().padding(vertical = 4.dp).height(8.dp)

        when (val uploadState = state.state) {
            is UploadMissingMeasurements.State.Uploading -> {
                Text(
                    text = stringResource(
                        Res.string.Results_UploadingMissing,
                        uploadState.progressText,
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

@Composable
private fun Stopping() {
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

@Composable
private fun RunningTests(
    state: RunBackgroundState.RunningTests,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onEvent(DashboardViewModel.Event.RunningTestClicked) }
            .padding(horizontal = 16.dp)
            .padding(top = 32.dp, bottom = 8.dp),
    ) {
        Row {
            Text(
                text = stringResource(Res.string.Dashboard_Running_Running),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 4.dp),
            )
            state.testName()?.let { testName ->
                state.testIcon()?.let { testIcon ->
                    Icon(
                        painterResource(testIcon),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp).size(24.dp),
                    )
                }
                Text(
                    text = testName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        state.progress.let { progress ->
            val color = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.onBackground
            val modifier =
                Modifier.fillMaxWidth().padding(vertical = 4.dp).height(8.dp)

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
                    text = " " + timeLeft.format(),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun RunBackgroundState.RunningTests.testName() =
    if (descriptor?.isWebConnectivityOnly == true) {
        descriptor.title()
    } else {
        testType?.displayName
    }

@Composable
private fun RunBackgroundState.RunningTests.testIcon() =
    (if (descriptor?.isWebConnectivityOnly == true) descriptor.icon else null)
        ?: testType?.iconRes

@Preview
@Composable
fun RunBackgroundIdlePreview() {
    AppTheme {
        Idle(
            state = RunBackgroundState.Idle,
            onEvent = {},
        )
    }
}

@Preview
@Composable
fun RunBackgroundUploadingMissingResultsPreview() {
    AppTheme {
        UploadingMissingResults(
            state = RunBackgroundState.UploadingMissingResults(
                UploadMissingMeasurements.State.Uploading(
                    uploaded = 2,
                    failedToUpload = 1,
                    total = 10,
                ),
            ),
        )
    }
}

@Preview
@Composable
fun RunBackgroundRunningTestsPreview() {
    AppTheme {
        RunningTests(
            state = RunBackgroundState.RunningTests(
                testType = TestType.Whatsapp,
            ),
            onEvent = {},
        )
    }
}

@Preview
@Composable
fun RunBackgroundStoppingPreview() {
    AppTheme {
        Stopping()
    }
}
