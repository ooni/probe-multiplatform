package org.ooni.probe.uitesting

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ooniprobe.composeapp.generated.resources.Modal_EnableNotifications_Title
import ooniprobe.composeapp.generated.resources.Onboarding_AutomatedTesting_Title
import ooniprobe.composeapp.generated.resources.Onboarding_Crash_Title
import ooniprobe.composeapp.generated.resources.Onboarding_DefaultSettings_Button_Go
import ooniprobe.composeapp.generated.resources.Onboarding_DefaultSettings_Title
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_True
import ooniprobe.composeapp.generated.resources.Onboarding_ThingsToKnow_Button
import ooniprobe.composeapp.generated.resources.Onboarding_ThingsToKnow_Title
import ooniprobe.composeapp.generated.resources.Onboarding_WhatIsOONIProbe_GotIt
import ooniprobe.composeapp.generated.resources.Onboarding_WhatIsOONIProbe_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.uitesting.helpers.clickOnTag
import org.ooni.probe.uitesting.helpers.clickOnText
import org.ooni.probe.uitesting.helpers.dependencies
import org.ooni.probe.uitesting.helpers.onNodeWithContentDescription
import org.ooni.probe.uitesting.helpers.onNodeWithText
import org.ooni.probe.uitesting.helpers.preferences
import org.ooni.probe.uitesting.helpers.start
import org.ooni.probe.uitesting.helpers.wait

@RunWith(AndroidJUnit4::class)
class OnboardingTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() =
        runTest {
            start()
        }

    @Test
    fun onboarding() =
        runTest {
            with(compose) {
                wait {
                    onNodeWithText(Res.string.Onboarding_WhatIsOONIProbe_Title)
                        .isDisplayed()
                }
                clickOnText(Res.string.Onboarding_WhatIsOONIProbe_GotIt)

                wait {
                    onNodeWithText(Res.string.Onboarding_ThingsToKnow_Title)
                        .isDisplayed()
                }
                clickOnText(Res.string.Onboarding_ThingsToKnow_Button)

                // Quiz
                clickOnText(Res.string.Onboarding_PopQuiz_True)
                clickOnText(Res.string.Onboarding_PopQuiz_True)

                wait { onNodeWithText(Res.string.Onboarding_AutomatedTesting_Title).isDisplayed() }
                clickOnTag("No-AutoTest")

                wait { onNodeWithText(Res.string.Onboarding_Crash_Title).isDisplayed() }
                clickOnTag("Yes-CrashReporting")

                if (dependencies.platformInfo.needsToRequestNotificationsPermission) {
                    wait {
                        onNodeWithText(Res.string.Modal_EnableNotifications_Title).isDisplayed()
                    }
                    clickOnTag("No-Notifications")
                }

                wait { onNodeWithText(Res.string.Onboarding_DefaultSettings_Title).isDisplayed() }
                clickOnText(Res.string.Onboarding_DefaultSettings_Button_Go)

                wait { onNodeWithContentDescription(Res.string.app_name).isDisplayed() }
            }

            assertEquals(
                false,
                preferences.getValueByKey(SettingsKey.AUTOMATED_TESTING_ENABLED).first(),
            )
            assertEquals(true, preferences.getValueByKey(SettingsKey.SEND_CRASH).first())
        }
}
