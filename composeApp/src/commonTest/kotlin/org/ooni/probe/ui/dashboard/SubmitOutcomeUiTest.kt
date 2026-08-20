package org.ooni.probe.ui.dashboard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import ooniprobe.composeapp.generated.resources.Common_Dismiss
import ooniprobe.composeapp.generated.resources.Dashboard_UpdatePrompt_Action
import ooniprobe.composeapp.generated.resources.Dashboard_UpdatePrompt_Description
import ooniprobe.composeapp.generated.resources.Dashboard_UpdatePrompt_Title
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.getString
import org.ooni.probe.ui.shared.UpdateRequiredDialog
import org.ooni.testing.TestLifecycleOwner
import kotlin.test.Test
import kotlin.test.assertTrue

class SubmitOutcomeUiTest {
    @Test
    fun showsUpdateRequiredBannerWhenUpdateIsRequired() =
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides TestLifecycleOwner(Lifecycle.State.RESUMED),
                ) {
                    DashboardScreen(
                        state = DashboardViewModel.State(updateRequired = true),
                        onEvent = {},
                    )
                }
            }

            onNodeWithText(getString(Res.string.Dashboard_UpdatePrompt_Title)).assertExists()
            onNodeWithText(getString(Res.string.Dashboard_UpdatePrompt_Description)).assertExists()
            onNodeWithText(getString(Res.string.Dashboard_UpdatePrompt_Action)).assertExists()
            onNodeWithText(getString(Res.string.Common_Dismiss)).assertExists()
        }

    @Test
    fun hidesUpdateRequiredBannerWhenNoUpdateIsRequired() =
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides TestLifecycleOwner(Lifecycle.State.RESUMED),
                ) {
                    DashboardScreen(
                        state = DashboardViewModel.State(updateRequired = false),
                        onEvent = {},
                    )
                }
            }

            onAllNodesWithText(getString(Res.string.Dashboard_UpdatePrompt_Title)).assertCountEquals(0)
        }

    @Test
    fun updateBannerUpdateActionEmitsUpdateClicked() =
        runComposeUiTest {
            val events = mutableListOf<DashboardViewModel.Event>()
            setContent {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides TestLifecycleOwner(Lifecycle.State.RESUMED),
                ) {
                    DashboardScreen(
                        state = DashboardViewModel.State(updateRequired = true),
                        onEvent = events::add,
                    )
                }
            }

            onNodeWithText(getString(Res.string.Dashboard_UpdatePrompt_Action)).performClick()

            assertTrue(events.any { it is DashboardViewModel.Event.UpdateClicked })
        }

    @Test
    fun updateBannerDismissActionEmitsUpdateDismissed() =
        runComposeUiTest {
            val events = mutableListOf<DashboardViewModel.Event>()
            setContent {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides TestLifecycleOwner(Lifecycle.State.RESUMED),
                ) {
                    DashboardScreen(
                        state = DashboardViewModel.State(updateRequired = true),
                        onEvent = events::add,
                    )
                }
            }

            onNodeWithText(getString(Res.string.Common_Dismiss)).performClick()

            assertTrue(events.any { it is DashboardViewModel.Event.UpdateDismissed })
        }

    @Test
    fun updateRequiredDialogShowsPromptAndActions() =
        runComposeUiTest {
            var updateClicked = false
            var dismissClicked = false
            setContent {
                UpdateRequiredDialog(
                    onUpdate = { updateClicked = true },
                    onDismiss = { dismissClicked = true },
                )
            }

            onNodeWithText(getString(Res.string.Dashboard_UpdatePrompt_Title)).assertExists()
            onNodeWithText(getString(Res.string.Dashboard_UpdatePrompt_Description)).assertExists()
            onNodeWithText(getString(Res.string.Dashboard_UpdatePrompt_Action)).performClick()
            onNodeWithText(getString(Res.string.Common_Dismiss)).performClick()

            assertTrue(updateClicked)
            assertTrue(dismissClicked)
        }
}
