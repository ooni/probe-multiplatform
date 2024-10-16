package org.ooni.probe.uitesting

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.uitesting.helpers.clickOnTag
import org.ooni.probe.uitesting.helpers.clickOnText
import org.ooni.probe.uitesting.helpers.dependencies
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
                wait { onNodeWithText("What is OONI Probe?").isDisplayed() }
                clickOnText("Got It")

                wait { onNodeWithText("Heads-up!").isDisplayed() }
                clickOnText("I understand")

                // Quiz
                clickOnText("True")
                clickOnText("True")

                wait { onNodeWithText("Automated testing").isDisplayed() }
                clickOnTag("No-AutoTest")

                wait { onNodeWithText("Crash Reporting").isDisplayed() }
                clickOnTag("Yes-CrashReporting")

                if (dependencies.platformInfo.needsToRequestNotificationsPermission) {
                    wait { onNodeWithText("Get updates on internet censorship").isDisplayed() }
                    clickOnTag("No-Notifications")
                }

                wait { onNodeWithText("Default Settings").isDisplayed() }
                clickOnText("Let’s go")

                wait { onNodeWithContentDescription("OONI Probe").isDisplayed() }
            }

            assertEquals(
                false,
                preferences.getValueByKey(SettingsKey.AUTOMATED_TESTING_ENABLED).first(),
            )
            assertEquals(true, preferences.getValueByKey(SettingsKey.SEND_CRASH).first())
        }
}
