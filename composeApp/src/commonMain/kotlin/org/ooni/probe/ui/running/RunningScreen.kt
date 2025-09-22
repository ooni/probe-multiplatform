package org.ooni.probe.ui.running

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Dashboard_Running_EstimatedTimeLeft
import ooniprobe.composeapp.generated.resources.Dashboard_Running_ProxyInUse
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Running
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Stopping_Notice
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Stopping_Title
import ooniprobe.composeapp.generated.resources.Notification_StopTest
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ooni_empty_state
import ooniprobe.composeapp.generated.resources.test_circumvention
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.ui.shared.LottieAnimation
import org.ooni.probe.ui.shared.NavigationBackButton
import org.ooni.probe.ui.shared.TestRunErrorMessages
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.format
import org.ooni.probe.ui.theme.customColors

@Composable
fun RunningScreen(
    state: RunningViewModel.State,
    onEvent: (RunningViewModel.Event) -> Unit,
) {
    val descriptorColor = (state.runBackgroundState as? RunBackgroundState.RunningTests)?.descriptor?.color
    val contentColor = MaterialTheme.customColors.onDescriptor
    Surface(
        color = descriptorColor ?: MaterialTheme.colorScheme.primaryContainer,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()),
        ) {
            TopBar(
                title = {},
                navigationIcon = {
                    NavigationBackButton({ onEvent(RunningViewModel.Event.BackClicked) })
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = contentColor,
                ),
            )

            when (state.runBackgroundState) {
                is RunBackgroundState.RunningTests -> TestRunning(state.hasProxy, state.runBackgroundState, onEvent)
                RunBackgroundState.Stopping -> TestStopping()
                else -> Unit
            }
        }
    }

    TestRunErrorMessages(
        errors = state.testRunErrors,
        onErrorDisplayed = { onEvent(RunningViewModel.Event.ErrorDisplayed(it)) },
    )
}

@Composable
private fun TestRunning(
    hasProxy: Boolean,
    state: RunBackgroundState.RunningTests,
    onEvent: (RunningViewModel.Event) -> Unit,
) {
    val contentColor = LocalContentColor.current

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        state.testType?.let { testType ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.Dashboard_Running_Running),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = testType.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (hasProxy) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = stringResource(Res.string.Dashboard_Running_ProxyInUse),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(Res.drawable.test_circumvention),
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                                tint = MaterialTheme.customColors.onDescriptor,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.customColors.onDescriptor,
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = MaterialTheme.customColors.onDescriptor,
                        ),
                    )
                }
            }
        }

        if (state.descriptor?.animation != null) {
            LottieAnimation(
                state.descriptor.animation,
                contentDescription = null,
                modifier = Modifier.size(160.dp),
            )
        } else {
            Icon(
                painterResource(state.descriptor?.icon ?: Res.drawable.ooni_empty_state),
                contentDescription = null,
                modifier = Modifier.size(160.dp).padding(24.dp),
            )
        }

        val progressTrackColor = contentColor.copy(alpha = 0.5f)
        val progressModifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(32.dp))

        if (state.progress == 0.0) {
            LinearProgressIndicator(
                color = contentColor,
                trackColor = progressTrackColor,
                modifier = progressModifier,
            )
        } else {
            LinearProgressIndicator(
                progress = { state.progress.toFloat() },
                color = contentColor,
                trackColor = progressTrackColor,
                modifier = progressModifier,
            )
        }

        state.estimatedTimeLeft?.let { timeLeft ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.Dashboard_Running_EstimatedTimeLeft),
                )
                Text(
                    text = timeLeft.format(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Text(
            state.log.orEmpty(),
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(56.dp),
        )

        OutlinedButton(
            onClick = { onEvent(RunningViewModel.Event.StopTestClicked) },
            border = ButtonDefaults
                .outlinedButtonBorder(enabled = true)
                .copy(brush = SolidColor(contentColor)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        ) {
            Text(stringResource(Res.string.Notification_StopTest))
        }
    }
}

@Composable
private fun TestStopping() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Text(
            text = stringResource(Res.string.Dashboard_Running_Stopping_Title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.Dashboard_Running_Stopping_Notice),
            textAlign = TextAlign.Center,
        )
    }
}
