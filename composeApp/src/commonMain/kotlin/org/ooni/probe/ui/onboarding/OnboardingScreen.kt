package org.ooni.probe.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Modal_Autorun_BatteryOptimization_Onboarding
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_OK
import ooniprobe.composeapp.generated.resources.Onboarding_AutomatedTesting_Paragraph
import ooniprobe.composeapp.generated.resources.Onboarding_AutomatedTesting_Title
import ooniprobe.composeapp.generated.resources.Onboarding_Crash_Button_No
import ooniprobe.composeapp.generated.resources.Onboarding_Crash_Button_Yes
import ooniprobe.composeapp.generated.resources.Onboarding_Crash_Paragraph
import ooniprobe.composeapp.generated.resources.Onboarding_Crash_Title
import ooniprobe.composeapp.generated.resources.Onboarding_DefaultSettings_Bullet_1
import ooniprobe.composeapp.generated.resources.Onboarding_DefaultSettings_Bullet_2
import ooniprobe.composeapp.generated.resources.Onboarding_DefaultSettings_Bullet_3
import ooniprobe.composeapp.generated.resources.Onboarding_DefaultSettings_Button_Change
import ooniprobe.composeapp.generated.resources.Onboarding_DefaultSettings_Button_Go
import ooniprobe.composeapp.generated.resources.Onboarding_DefaultSettings_Header
import ooniprobe.composeapp.generated.resources.Onboarding_DefaultSettings_Paragraph
import ooniprobe.composeapp.generated.resources.Onboarding_DefaultSettings_Title
import ooniprobe.composeapp.generated.resources.Onboarding_Notifications_Skip
import ooniprobe.composeapp.generated.resources.Onboarding_Notifications_Ok
import ooniprobe.composeapp.generated.resources.Onboarding_Notifications_Paragraph
import ooniprobe.composeapp.generated.resources.Onboarding_Notifications_Title
import ooniprobe.composeapp.generated.resources.Onboarding_ThingsToKnow_Bullet_1
import ooniprobe.composeapp.generated.resources.Onboarding_ThingsToKnow_Bullet_2
import ooniprobe.composeapp.generated.resources.Onboarding_ThingsToKnow_Bullet_3
import ooniprobe.composeapp.generated.resources.Onboarding_ThingsToKnow_Button
import ooniprobe.composeapp.generated.resources.Onboarding_ThingsToKnow_LearnMore
import ooniprobe.composeapp.generated.resources.Onboarding_ThingsToKnow_Title
import ooniprobe.composeapp.generated.resources.Onboarding_WhatIsOONIProbe_GotIt
import ooniprobe.composeapp.generated.resources.Onboarding_WhatIsOONIProbe_Paragraph
import ooniprobe.composeapp.generated.resources.Onboarding_WhatIsOONIProbe_Title
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.ui.shared.MarkdownViewer
import org.ooni.probe.ui.shared.Permission
import org.ooni.probe.ui.shared.PermissionDeniedAlwaysException
import org.ooni.probe.ui.shared.PermissionDeniedException
import org.ooni.probe.ui.shared.PermissionRequestCanceledException
import org.ooni.probe.ui.shared.buildPermissionsController
import org.ooni.probe.ui.shared.isHeightCompact
import org.ooni.probe.ui.shared.isWidthCompact
import org.ooni.probe.ui.theme.LocalCustomColors

@Composable
fun OnboardingScreen(
    state: OnboardingViewModel.State,
    onEvent: (OnboardingViewModel.Event) -> Unit,
) {
    var showQuiz by remember { mutableStateOf(false) }

    Box {
        Surface(
            color = state.step.surfaceColor,
            contentColor = LocalCustomColors.current.onOnboarding,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(bottom = 48.dp),
            ) {
                when (state.step) {
                    OnboardingViewModel.Step.WhatIs ->
                        WhatIsStep(onEvent)

                    OnboardingViewModel.Step.HeadsUp ->
                        HeadsUpStep(
                            onEvent = onEvent,
                            onShowQuiz = { showQuiz = true },
                        )

                    is OnboardingViewModel.Step.AutomatedTesting ->
                        AutomatedTestingStep(state.step.showBatteryOptimizationDialog, onEvent)

                    OnboardingViewModel.Step.CrashReporting ->
                        CrashReportingStep(onEvent)

                    OnboardingViewModel.Step.RequestNotificationPermission ->
                        RequestPermissionStep(onEvent)

                    OnboardingViewModel.Step.DefaultSettings ->
                        DefaultSettingsStep(onEvent)
                }
            }
        }

        Row(
            Modifier
                .wrapContentHeight()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .padding(WindowInsets.navigationBars.asPaddingValues()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(state.totalSteps) { index ->
                if (index != 0) {
                    Box(
                        modifier = Modifier
                            .alpha(if (state.stepIndex >= index) 1f else 0.33f)
                            .background(LocalCustomColors.current.onOnboarding)
                            .height(2.dp)
                            .width(36.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .alpha(if (state.stepIndex >= index) 1f else 0.33f)
                        .padding(1.dp)
                        .clip(CircleShape)
                        .background(LocalCustomColors.current.onOnboarding)
                        .size(16.dp),
                )
            }
        }
    }

    if (showQuiz) {
        OnboardingQuiz(
            onBack = { showQuiz = false },
            onFinish = {
                showQuiz = false
                onEvent(OnboardingViewModel.Event.NextClicked)
            },
        )
    }
}

@Composable
fun ColumnScope.WhatIsStep(onEvent: (OnboardingViewModel.Event) -> Unit) {
    OnboardingImage(OrganizationConfig.onboardingImages.image1)

    Box(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            OnboardingTitle(Res.string.Onboarding_WhatIsOONIProbe_Title)
            OnboardingText(Res.string.Onboarding_WhatIsOONIProbe_Paragraph)
        }
        OnboardingMainButton(
            text = Res.string.Onboarding_WhatIsOONIProbe_GotIt,
            onClick = { onEvent(OnboardingViewModel.Event.NextClicked) },
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 64.dp),
        )
    }
}

@Composable
fun ColumnScope.HeadsUpStep(
    onEvent: (OnboardingViewModel.Event) -> Unit,
    onShowQuiz: () -> Unit,
) {
    OnboardingImage(OrganizationConfig.onboardingImages.image2)

    Box(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            OnboardingTitle(Res.string.Onboarding_ThingsToKnow_Title)

            OnboardingBulletText(Res.string.Onboarding_ThingsToKnow_Bullet_1)
            OnboardingBulletText(Res.string.Onboarding_ThingsToKnow_Bullet_2)
            OnboardingBulletText(Res.string.Onboarding_ThingsToKnow_Bullet_3)
        }

        Column(
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter),
        ) {
            OnboardingMainButton(
                text = Res.string.Onboarding_ThingsToKnow_Button,
                onClick = { onShowQuiz() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 64.dp),
            )
            OnboardingTextButton(
                text = Res.string.Onboarding_ThingsToKnow_LearnMore,
                onClick = { onEvent(OnboardingViewModel.Event.HeadsUpLearnMoreClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 64.dp),
            )
        }
    }
}

@Composable
fun ColumnScope.AutomatedTestingStep(
    showBatteryOptimizationDialog: Boolean,
    onEvent: (OnboardingViewModel.Event) -> Unit,
) {
    OnboardingImage(OrganizationConfig.onboardingImages.image3)

    Box(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            OnboardingTitle(Res.string.Onboarding_AutomatedTesting_Title)
            OnboardingText(Res.string.Onboarding_AutomatedTesting_Paragraph)
        }

        Row(modifier = Modifier.padding(horizontal = 8.dp).align(alignment = Alignment.BottomCenter)) {
            OnboardingMainOutlineButton(
                text = Res.string.Onboarding_Crash_Button_No,
                onClick = { onEvent(OnboardingViewModel.Event.AutoTestNoClicked) },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f)
                    .testTag("No-AutoTest"),
            )
            OnboardingMainButton(
                text = Res.string.Onboarding_Crash_Button_Yes,
                onClick = { onEvent(OnboardingViewModel.Event.AutoTestYesClicked) },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
            )
        }
    }

    if (showBatteryOptimizationDialog) {
        AlertDialog(
            onDismissRequest = { },
            text = {
                Text(stringResource(Res.string.Modal_Autorun_BatteryOptimization_Onboarding))
            },
            confirmButton = {
                OutlinedButton(onClick = {
                    onEvent(OnboardingViewModel.Event.BatteryOptimizationOkClicked)
                }) {
                    Text(stringResource(Res.string.Modal_OK))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onEvent(OnboardingViewModel.Event.BatteryOptimizationCancelClicked)
                }) {
                    Text(stringResource(Res.string.Modal_Cancel))
                }
            },
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false,
            ),
        )
    }
}

@Composable
fun ColumnScope.CrashReportingStep(onEvent: (OnboardingViewModel.Event) -> Unit) {
    OnboardingImage(OrganizationConfig.onboardingImages.image3)

    Box(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            OnboardingTitle(Res.string.Onboarding_Crash_Title)
            OnboardingText(Res.string.Onboarding_Crash_Paragraph)
        }

        Row(modifier = Modifier.padding(horizontal = 8.dp).align(alignment = Alignment.BottomCenter)) {
            OnboardingMainOutlineButton(
                text = Res.string.Onboarding_Crash_Button_No,
                onClick = { onEvent(OnboardingViewModel.Event.CrashReportingNoClicked) },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
            )
            OnboardingMainButton(
                text = Res.string.Onboarding_Crash_Button_Yes,
                onClick = { onEvent(OnboardingViewModel.Event.CrashReportingYesClicked) },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f)
                    .testTag("Yes-CrashReporting"),
            )
        }
    }
}

@Composable
fun ColumnScope.RequestPermissionStep(onEvent: (OnboardingViewModel.Event) -> Unit) {
    val permissionsController = buildPermissionsController()
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    OnboardingImage(OrganizationConfig.onboardingImages.image3)
    Box(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            OnboardingTitle(Res.string.Onboarding_Notifications_Title)
            OnboardingText(Res.string.Onboarding_Notifications_Paragraph)
        }

        Row(modifier = Modifier.padding(horizontal = 8.dp).align(alignment = Alignment.BottomCenter)) {
            OnboardingMainOutlineButton(
                text = Res.string.Onboarding_Notifications_Skip,
                onClick = { onEvent(OnboardingViewModel.Event.RequestNotificationsPermissionSkipped) },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f)
                    .testTag("No-Notifications"),
            )
            OnboardingMainButton(
                text = Res.string.Onboarding_Notifications_Ok,
                onClick = {
                    coroutineScope.launch {
                        try {
                            permissionsController.providePermission(Permission.RemoteNotification)
                            Logger.i("Notifications permission accepted")
                        } catch (e: PermissionDeniedException) {
                            Logger.i("Notifications permission denied")
                            onEvent(OnboardingViewModel.Event.NextClicked)
                        } catch (e: PermissionDeniedAlwaysException) {
                            Logger.i("Notifications permission already denied")
                            onEvent(OnboardingViewModel.Event.NextClicked)
                        } catch (e: PermissionRequestCanceledException) {
                            Logger.i("Notifications permission request cancelled")
                            // Nothing to do here
                        } catch (e: Exception) {
                            Logger.e("Error requesting notifications permission", e)
                        }
                        onEvent(OnboardingViewModel.Event.RequestNotificationsPermissionDone)
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
            )
        }
    }
}

@Composable
fun ColumnScope.DefaultSettingsStep(onEvent: (OnboardingViewModel.Event) -> Unit) {
    OnboardingImage(OrganizationConfig.onboardingImages.image3)

    Box(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            OnboardingTitle(Res.string.Onboarding_DefaultSettings_Title)

            OnboardingText(Res.string.Onboarding_DefaultSettings_Header)
            OnboardingBulletText(Res.string.Onboarding_DefaultSettings_Bullet_1)
            OnboardingBulletText(Res.string.Onboarding_DefaultSettings_Bullet_2)
            OnboardingBulletText(Res.string.Onboarding_DefaultSettings_Bullet_3)
            OnboardingText(Res.string.Onboarding_DefaultSettings_Paragraph)
        }

        Column(
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter),
        ) {
            OnboardingMainButton(
                text = Res.string.Onboarding_DefaultSettings_Button_Go,
                onClick = { onEvent(OnboardingViewModel.Event.NextClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 64.dp),
            )
            OnboardingTextButton(
                text = Res.string.Onboarding_DefaultSettings_Button_Change,
                onClick = { onEvent(OnboardingViewModel.Event.ChangeDefaultsClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 64.dp),
            )
        }
    }
}

@Composable
private fun OnboardingImage(
    image: DrawableResource,
    modifier: Modifier = Modifier,
) {
    if (isHeightCompact()) {
        Spacer(
            modifier = modifier.padding(WindowInsets.statusBars.asPaddingValues()),
        )
    } else {
        Image(
            painterResource(image),
            contentDescription = null,
            contentScale = if (isWidthCompact()) ContentScale.FillWidth else ContentScale.Inside,
            modifier = Modifier.fillMaxWidth()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .run {
                    if (!isWidthCompact()) sizeIn(maxHeight = 400.dp) else this
                },
        )
    }
}

@Composable
private fun OnboardingTitle(text: StringResource) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp),
    )
}

@Composable
private fun OnboardingText(text: StringResource) {
    MarkdownViewer(
        markdown = stringResource(text),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
    )
}

@Composable
private fun OnboardingBulletText(text: StringResource) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Text(
            "•",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 2.dp, end = 4.dp),
        )
        MarkdownViewer(
            markdown = stringResource(text),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OnboardingMainButton(
    text: StringResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            contentColor = OnboardingViewModel.Step.WhatIs.surfaceColor,
            containerColor = LocalContentColor.current,
        ),
        modifier = modifier.requiredSizeIn(minHeight = if (isHeightCompact()) 48.dp else 60.dp),
    ) {
        Text(
            stringResource(text),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun OnboardingMainOutlineButton(
    text: StringResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalContentColor.current),
        border = BorderStroke(width = 2.dp, color = LocalContentColor.current),
        modifier = modifier.requiredSizeIn(minHeight = if (isHeightCompact()) 48.dp else 60.dp),
    ) {
        Text(
            stringResource(text),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun OnboardingTextButton(
    text: StringResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = LocalContentColor.current,
        ),
        modifier = modifier,
    ) {
        Text(
            stringResource(text),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private val OnboardingViewModel.Step.surfaceColor
    @Composable get() = when (this) {
        OnboardingViewModel.Step.WhatIs -> LocalCustomColors.current.onboarding1
        OnboardingViewModel.Step.HeadsUp -> LocalCustomColors.current.onboarding2
        else -> LocalCustomColors.current.onboarding3
    }
