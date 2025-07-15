package org.ooni.probe.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Common_Clear
import ooniprobe.composeapp.generated.resources.Res
import org.ooni.probe.config.OrganizationConfig

import ooniprobe.composeapp.generated.resources.Onboarding_CleanUp_Paragraph
import ooniprobe.composeapp.generated.resources.Onboarding_CleanUp_Title
import ooniprobe.composeapp.generated.resources.Onboarding_Crash_Button_No

@Composable
fun ColumnScope.CleanUpStep(onEvent: (OnboardingViewModel.Event) -> Unit) {
    OnboardingImage(OrganizationConfig.onboardingImages.image1)

    Box(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            OnboardingTitle(Res.string.Onboarding_CleanUp_Title)
            OnboardingText(Res.string.Onboarding_CleanUp_Paragraph)
        }
        Row(modifier = Modifier.padding(horizontal = 8.dp).align(alignment = Alignment.BottomCenter)) {
            OnboardingMainOutlineButton(
                text = Res.string.Onboarding_Crash_Button_No,
                onClick = { onEvent(OnboardingViewModel.Event.NextClicked) },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
            )

            OnboardingMainButton(
                text = Res.string.Common_Clear,
                onClick = { onEvent(OnboardingViewModel.Event.CleanupClicked) },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
            )
        }
    }
}
