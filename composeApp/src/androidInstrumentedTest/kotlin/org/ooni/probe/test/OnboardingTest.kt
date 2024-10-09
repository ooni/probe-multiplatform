package org.ooni.probe.test

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
import org.ooni.probe.test.helpers.CleanTestRule
import org.ooni.probe.test.helpers.FlakyTestRule
import org.ooni.probe.test.helpers.clickOnTag
import org.ooni.probe.test.helpers.clickOnText
import org.ooni.probe.test.helpers.dependencies
import org.ooni.probe.test.helpers.preferences
import org.ooni.probe.test.helpers.start
import org.ooni.probe.test.helpers.wait

@RunWith(AndroidJUnit4::class)
class OnboardingTest {
    @get:Rule
    val clean = CleanTestRule()

    @get:Rule
    val flakyTestRule = FlakyTestRule()

    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() {
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
                clickOnTag("Yes-AutoTest")

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
                true,
                preferences.getValueByKey(SettingsKey.AUTOMATED_TESTING_ENABLED).first(),
            )
            assertEquals(true, preferences.getValueByKey(SettingsKey.SEND_CRASH).first())
        }
}
