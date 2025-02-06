package org.ooni.probe.uitesting.screenshots

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import ooniprobe.composeapp.generated.resources.Modal_EnableNotifications_Title
import ooniprobe.composeapp.generated.resources.OONIRun_Run
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
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.cleanstatusbar.BluetoothState
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar
import tools.fastlane.screengrab.cleanstatusbar.MobileDataType
import tools.fastlane.screengrab.locale.LocaleTestRule
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class AutomateScreenshotsTest {
    @Rule @JvmField
    val localeTestRule = LocaleTestRule()
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() =
        runTest {
            preferences.setValueByKey(SettingsKey.FIRST_RUN, true)
            start()
            CleanStatusBar()
                .setBluetoothState(BluetoothState.DISCONNECTED)
                .setMobileNetworkDataType(MobileDataType.LTE)
                .enable()
        }

    @Test
    fun onboarding() =
        runTest {
            with(compose) {
                wait(timeout = 30.seconds) {
                    onNodeWithText(Res.string.Onboarding_WhatIsOONIProbe_Title)
                        .isDisplayed()
                }
                Screengrab.screenshot("00-what-is-ooni-probe")

                clickOnText(Res.string.Onboarding_WhatIsOONIProbe_GotIt)

                wait {
                    onNodeWithText(Res.string.Onboarding_ThingsToKnow_Title)
                        .isDisplayed()
                }
                Screengrab.screenshot("01-things-to-know")
                clickOnText(Res.string.Onboarding_ThingsToKnow_Button)

                // Quiz
                clickOnText(Res.string.Onboarding_PopQuiz_True)
                clickOnText(Res.string.Onboarding_PopQuiz_True)

                wait { onNodeWithText(Res.string.Onboarding_AutomatedTesting_Title).isDisplayed() }
                Screengrab.screenshot("02-automated-testing")
                clickOnTag("No-AutoTest")

                wait { onNodeWithText(Res.string.Onboarding_Crash_Title).isDisplayed() }
                Screengrab.screenshot("03-crash-reporting")
                clickOnTag("Yes-CrashReporting")

                if (dependencies.platformInfo.needsToRequestNotificationsPermission) {
                    wait {
                        onNodeWithText(Res.string.Modal_EnableNotifications_Title).isDisplayed()
                    }
                    Screengrab.screenshot("04-enable-notifications")
                    clickOnTag("No-Notifications")
                }

                wait { onNodeWithText(Res.string.Onboarding_DefaultSettings_Title).isDisplayed() }
                Screengrab.screenshot("05-default-settings")
                clickOnText(Res.string.Onboarding_DefaultSettings_Button_Go)

                wait { onNodeWithContentDescription(Res.string.app_name).isDisplayed() }

                Screengrab.screenshot("06-dashboard")

                wait(timeout = 30.seconds) {
                    onNodeWithText(Res.string.OONIRun_Run)
                        .isDisplayed()
                }

                clickOnText(Res.string.OONIRun_Run)

                Screengrab.screenshot("07-run-tests")

                clickOnTag("Run-Button")

                Screengrab.screenshot("08-dashboard-running")
            }
        }
}
