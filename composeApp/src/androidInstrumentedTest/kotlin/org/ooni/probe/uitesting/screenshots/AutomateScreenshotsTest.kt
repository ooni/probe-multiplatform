package org.ooni.probe.uitesting.screenshots

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import ooniprobe.composeapp.generated.resources.Common_Back
import ooniprobe.composeapp.generated.resources.Dashboard_Progress_UpdateLink_Label
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
import ooniprobe.composeapp.generated.resources.Settings_About_Label
import ooniprobe.composeapp.generated.resources.Settings_Advanced_Label
import ooniprobe.composeapp.generated.resources.Settings_Notifications_Label
import ooniprobe.composeapp.generated.resources.Settings_Privacy_Label
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Enabled
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Label
import ooniprobe.composeapp.generated.resources.Settings_Sharing_UploadResults_Description
import ooniprobe.composeapp.generated.resources.Settings_TestOptions_Label
import ooniprobe.composeapp.generated.resources.Settings_Title
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Label
import ooniprobe.composeapp.generated.resources.app_name
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.uitesting.helpers.clickOnContentDescription
import org.ooni.probe.uitesting.helpers.clickOnTag
import org.ooni.probe.uitesting.helpers.clickOnText
import org.ooni.probe.uitesting.helpers.dependencies
import org.ooni.probe.uitesting.helpers.isOoni
import org.ooni.probe.uitesting.helpers.onNodeWithContentDescription
import org.ooni.probe.uitesting.helpers.onNodeWithText
import org.ooni.probe.uitesting.helpers.preferences
import org.ooni.probe.uitesting.helpers.skipOnboarding
import org.ooni.probe.uitesting.helpers.start
import org.ooni.probe.uitesting.helpers.wait
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar
import tools.fastlane.screengrab.locale.LocaleTestRule
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class AutomateScreenshotsTest {
    @Rule
    @JvmField
    val localeTestRule = LocaleTestRule()

    @get:Rule
    val compose = createEmptyComposeRule()

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeAll() {
            CleanStatusBar.enableWithDefaults()
        }

        @AfterClass
        @JvmStatic
        fun afterAll() {
            CleanStatusBar.disable()
        }
    }

    @Test
    fun onboarding() =
        runTest {
            preferences.setValueByKey(SettingsKey.FIRST_RUN, true)
            start()

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
            }
        }

    @Test
    fun runTests() =
        runTest {
            skipOnboarding()
            start()
            with(compose) {
                wait { onNodeWithContentDescription(Res.string.app_name).isDisplayed() }

                wait(timeout = 30.seconds) {
                    onNodeWithText(Res.string.Dashboard_Progress_UpdateLink_Label)
                        .isNotDisplayed()
                }

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

    @Test
    fun settings() =
        runTest {
            skipOnboarding()
            start()
            with(compose) {
                wait { onNodeWithContentDescription(Res.string.app_name).isDisplayed() }
                clickOnText(Res.string.Settings_Title)

                wait { onNodeWithText(Res.string.Settings_About_Label).isDisplayed() }
                Screengrab.screenshot("09-settings")

                clickOnText(Res.string.Settings_Notifications_Label)

                wait { onNodeWithText(Res.string.Settings_Notifications_Label).isDisplayed() }
                Screengrab.screenshot("10-notifications")

                // back
                clickOnContentDescription(Res.string.Common_Back)

                wait { onNodeWithText(Res.string.Settings_About_Label).isDisplayed() }

                clickOnText(Res.string.Settings_TestOptions_Label)
                wait {
                    onNodeWithText(Res.string.Settings_TestOptions_Label).isDisplayed() &&
                        onNodeWithText(Res.string.Settings_Sharing_UploadResults_Description).isDisplayed()
                }

                Screengrab.screenshot("11-test-options")

                if (isOoni) {
                    clickOnText(Res.string.Settings_Websites_Categories_Label)
                    wait { onNodeWithText(Res.string.Settings_Websites_Categories_Label).isDisplayed() }

                    Screengrab.screenshot("12-websites-categories")

                    // back
                    clickOnContentDescription(Res.string.Common_Back)
                }

                clickOnContentDescription(Res.string.Common_Back)

                wait { onNodeWithText(Res.string.Settings_About_Label).isDisplayed() }

                clickOnText(Res.string.Settings_Privacy_Label)
                wait { onNodeWithText(Res.string.Settings_Privacy_Label).isDisplayed() }

                Screengrab.screenshot("13-privacy")

                // back
                clickOnContentDescription(Res.string.Common_Back)

                wait { onNodeWithText(Res.string.Settings_About_Label).isDisplayed() }

                clickOnText(Res.string.Settings_Proxy_Label)
                wait { onNodeWithText(Res.string.Settings_Proxy_Enabled).isDisplayed() }

                Screengrab.screenshot("14-proxy")

                // back
                clickOnContentDescription(Res.string.Common_Back)

                wait { onNodeWithText(Res.string.Settings_About_Label).isDisplayed() }

                clickOnText(Res.string.Settings_Advanced_Label)
                wait { onNodeWithText(Res.string.Settings_Advanced_Label).isDisplayed() }

                Screengrab.screenshot("15-advanced")

                // back
                clickOnContentDescription(Res.string.Common_Back)

                wait { onNodeWithText(Res.string.Settings_About_Label).isDisplayed() }

                clickOnText(Res.string.Settings_About_Label)

                wait { onNodeWithContentDescription(Res.string.Common_Back).isDisplayed() }

                Screengrab.screenshot("16-about")
            }
        }
}
