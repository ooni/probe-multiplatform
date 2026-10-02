package org.ooni.probe.ui.settings.proxy

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_HidePassword
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_ShowPassword
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AddProxyScreenTest {
    @Test
    fun togglingPasswordVisibilityEmitsEvent() {
        var toggled = false
        runComposeUiTest {
            setContent {
                AddProxyScreen(
                    state = AddProxyViewModel.State(
                        isPasswordVisible = false,
                    ),
                    onEvent = { event ->
                        if (event is AddProxyViewModel.Event.TogglePasswordVisibility) {
                            toggled = true
                        }
                    },
                )
            }

            onNodeWithTag("AddProxy-TogglePasswordVisibility").assertIsDisplayed()
            onNodeWithContentDescription(getString(Res.string.Settings_Proxy_Custom_ShowPassword)).assertIsDisplayed()
            onNodeWithTag("AddProxy-TogglePasswordVisibility").performClick()
            assertEquals(true, toggled)
        }
    }

    @Test
    fun passwordVisibleShowsHidePasswordContentDescription() {
        runComposeUiTest {
            setContent {
                AddProxyScreen(
                    state = AddProxyViewModel.State(
                        isPasswordVisible = true,
                    ),
                    onEvent = {},
                )
            }

            onNodeWithContentDescription(getString(Res.string.Settings_Proxy_Custom_HidePassword)).assertIsDisplayed()
        }
    }
}
