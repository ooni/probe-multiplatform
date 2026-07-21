package org.ooni.probe.ui.settings.credentials

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Measurements_Verification_Verified
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Reset_Title
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Status_Ready
import org.jetbrains.compose.resources.getString
import org.ooni.probe.domain.credentials.AnonymousCredentialsHealth
import kotlin.test.Test
import kotlin.test.assertEquals

class AnonymousCredentialsScreenTest {
    @Test
    fun showsReadyProbeIdAndConfirmsReset() =
        runComposeUiTest {
            val events = mutableListOf<AnonymousCredentialsViewModel.Event>()

            setContent {
                AnonymousCredentialsScreen(
                    state = AnonymousCredentialsViewModel.State(
                        health = AnonymousCredentialsHealth.Ready(
                            probeId = "probe-id",
                            probeAsn = "AS123",
                            probeCc = "CM",
                        ),
                        isLoading = false,
                    ),
                    onEvent = events::add,
                )
            }

            onNodeWithText(getString(Res.string.Settings_AnonymousCredentials_Status_Ready)).assertExists()
            onNodeWithContentDescription(getString(Res.string.Measurements_Verification_Verified))
                .assertExists()
            onNodeWithText("probe-id").assertExists()
            onNodeWithTag("AnonymousCredentials-Reset").performClick()
            onNodeWithText(getString(Res.string.Settings_AnonymousCredentials_Reset_Title)).assertExists()
            onNodeWithTag("AnonymousCredentials-ConfirmReset").performClick()

            assertEquals(AnonymousCredentialsViewModel.Event.ResetConfirmed, events.last())
        }
}
