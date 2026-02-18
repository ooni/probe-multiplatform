package org.ooni.probe.screenshots

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ooniprobe.composeapp.generated.resources.Common_Back
import ooniprobe.composeapp.generated.resources.Dashboard_Overview_ChooseWebsites
import ooniprobe.composeapp.generated.resources.Dashboard_Progress_UpdateLink_Label
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Running
import ooniprobe.composeapp.generated.resources.Dashboard_Tab_Label
import ooniprobe.composeapp.generated.resources.Notification_StopTest
import ooniprobe.composeapp.generated.resources.OONIRun_Run
import ooniprobe.composeapp.generated.resources.Onboarding_AutomatedTesting_Title
import ooniprobe.composeapp.generated.resources.Onboarding_Crash_Title
import ooniprobe.composeapp.generated.resources.Onboarding_DefaultSettings_Button_Go
import ooniprobe.composeapp.generated.resources.Onboarding_DefaultSettings_Title
import ooniprobe.composeapp.generated.resources.Onboarding_Notifications_Title
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_True
import ooniprobe.composeapp.generated.resources.Onboarding_ThingsToKnow_Button
import ooniprobe.composeapp.generated.resources.Onboarding_ThingsToKnow_Title
import ooniprobe.composeapp.generated.resources.Onboarding_WhatIsOONIProbe_GotIt
import ooniprobe.composeapp.generated.resources.Onboarding_WhatIsOONIProbe_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_About_Label
import ooniprobe.composeapp.generated.resources.Settings_Advanced_Label
import ooniprobe.composeapp.generated.resources.Settings_Privacy_Label
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Label
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Psiphon
import ooniprobe.composeapp.generated.resources.Settings_Sharing_UploadResults_Description
import ooniprobe.composeapp.generated.resources.Settings_TestOptions_Label
import ooniprobe.composeapp.generated.resources.Settings_Title
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Label
import ooniprobe.composeapp.generated.resources.Settings_Websites_CustomURL_Title
import ooniprobe.composeapp.generated.resources.TestResults
import ooniprobe.composeapp.generated.resources.Test_Dash_Fullname
import ooniprobe.composeapp.generated.resources.Tests_Title
import ooniprobe.composeapp.generated.resources.app_name
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.probe.data.models.OoniTest
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.getCurrent
import org.ooni.probe.uitesting.helpers.checkTextAnywhereInsideWebView
import org.ooni.probe.uitesting.helpers.clickOnContentDescription
import org.ooni.probe.uitesting.helpers.clickOnTag
import org.ooni.probe.uitesting.helpers.clickOnText
import org.ooni.probe.uitesting.helpers.defaultSettings
import org.ooni.probe.uitesting.helpers.dependencies
import org.ooni.probe.uitesting.helpers.getOoniDescriptor
import org.ooni.probe.uitesting.helpers.isNewsMediaScan
import org.ooni.probe.uitesting.helpers.isOoni
import org.ooni.probe.uitesting.helpers.onNodeWithContentDescription
import org.ooni.probe.uitesting.helpers.onNodeWithText
import org.ooni.probe.uitesting.helpers.preferences
import org.ooni.probe.uitesting.helpers.skipOnboarding
import org.ooni.probe.uitesting.helpers.start
import org.ooni.probe.uitesting.helpers.wait
import org.ooni.testing.factories.DatabaseHelper
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
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
            DatabaseHelper.initialize(dependencies)
            Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
            CleanStatusBar.enableWithDefaults()
        }

        @AfterClass
        @JvmStatic
        fun afterAll() {
            CleanStatusBar.disable()
        }
    }

    @Before
    fun setUp() =
        runTest {
            preferences.setValueByKey(SettingsKey.SEND_CRASH, false)
        }

    @Test
    fun onboarding() =
        runTest {
            if (!isOoni) return@runTest
            preferences.setValueByKey(SettingsKey.FIRST_RUN, true)
            preferences.setValueByKey(SettingsKey.TESTS_MOVED_NOTICE, true)
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

                if (dependencies.platformInfo.requestNotificationsPermission) {
                    wait {
                        onNodeWithText(Res.string.Onboarding_Notifications_Title).isDisplayed()
                    }
                    Screengrab.screenshot("04-enable-notifications")
                    clickOnTag("No-Notifications")
                }

                wait { onNodeWithText(Res.string.Onboarding_DefaultSettings_Title).isDisplayed() }
                Screengrab.screenshot("05-default-settings")
                clickOnText(Res.string.Onboarding_DefaultSettings_Button_Go)

                wait { onNodeWithContentDescription(Res.string.app_name).isDisplayed() }
                Screengrab.screenshot("1_" + locale())
            }
        }

    @Test
    fun tests() =
        runTest {
            if (!isOoni) return@runTest
            skipOnboarding()
            defaultSettings()
            start()

            with(compose) {
                wait { onNodeWithContentDescription(Res.string.app_name).isDisplayed() }

                wait(timeout = 30.seconds) {
                    onNodeWithText(Res.string.Dashboard_Progress_UpdateLink_Label)
                        .isNotDisplayed()
                }

                clickOnText(Res.string.Tests_Title)

                wait {
                    onNodeWithText(getOoniDescriptor(OoniTest.Websites).title()).isDisplayed()
                }
                Screengrab.screenshot("2_" + locale())
                Screengrab.screenshot("22-tests")
            }
        }

    @Test
    fun runTests() =
        runTest {
            if (!isOoni) return@runTest
            skipOnboarding()
            defaultSettings()
            start()
            with(compose) {
                wait { onNodeWithContentDescription(Res.string.app_name).isDisplayed() }

                wait(timeout = 30.seconds) {
                    onNodeWithText(Res.string.Dashboard_Progress_UpdateLink_Label)
                        .isNotDisplayed()
                }

                if (onNodeWithText(Res.string.Dashboard_Running_Running).isDisplayed()) {
                    Screengrab.screenshot("06-dashboard-running")

                    clickOnText(Res.string.Dashboard_Running_Running)
                    wait(timeout = 30.seconds) {
                        onNodeWithText(Res.string.Notification_StopTest)
                            .isDisplayed()
                    }

                    Screengrab.screenshot("06-running-running")

                    clickOnText(Res.string.Notification_StopTest)

                    wait(timeout = 30.seconds) {
                        onNodeWithText(Res.string.Dashboard_Tab_Label)
                            .isDisplayed()
                    }

                    clickOnText(Res.string.Dashboard_Tab_Label)

                    Thread.sleep(300)

                    Screengrab.screenshot("1_" + locale())

                    Screengrab.screenshot("06-dashboard")
                } else {
                    Screengrab.screenshot("1_" + locale())

                    Screengrab.screenshot("06-dashboard")
                }

                wait(timeout = 30.seconds) {
                    onNodeWithText(Res.string.OONIRun_Run)
                        .isDisplayed()
                }

                clickOnText(Res.string.OONIRun_Run)
                clickOnTag("Run-Button")

                Thread.sleep(3000)
                Screengrab.screenshot("07-run-tests")
            }
        }

    @Test
    fun settings() =
        runTest {
            if (!isOoni) return@runTest
            skipOnboarding()
            defaultSettings()
            start()
            with(compose) {
                wait { onNodeWithContentDescription(Res.string.app_name).isDisplayed() }
                clickOnText(Res.string.Settings_Title)

                wait { onNodeWithText(Res.string.Settings_About_Label).isDisplayed() }
                Screengrab.screenshot("09-settings")

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
                wait { onNodeWithText(Res.string.Settings_Proxy_Psiphon).isDisplayed() }

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

    @Test
    fun ooniResults() =
        runTest {
            if (!isOoni) return@runTest
            skipOnboarding()
            DatabaseHelper.setup()
            start()
            val websitesTitle = getOoniDescriptor(OoniTest.Websites).title()
            with(compose) {
                wait { onNodeWithContentDescription(Res.string.app_name).isDisplayed() }

                clickOnText(Res.string.TestResults)

                wait { onNodeWithText(websitesTitle).isDisplayed() }

                Screengrab.screenshot("17-results")

                Thread.sleep(3000)
                Screengrab.screenshot("3_" + locale())

                clickOnText(websitesTitle)

                wait(10.seconds) { onNodeWithText("https://z-lib.org/").isDisplayed() }

                // Screenshot was coming up empty, so we need to explicitly sleep here
                Thread.sleep(3000)
                Screengrab.screenshot("18-websites-results")

                clickOnText("https://z-lib.org/")

                checkTextAnywhereInsideWebView("https://z-lib.org/")

                Screengrab.screenshot("19-website-measurement-anomaly")
                Screengrab.screenshot("4_" + locale())

                clickOnContentDescription(Res.string.Common_Back)
                wait { onNodeWithText(websitesTitle).isDisplayed() }
                clickOnContentDescription(Res.string.Common_Back)
                wait { onNodeWithText(websitesTitle).isDisplayed() }
                clickOnText(websitesTitle)
                wait { onNodeWithText(Res.string.Test_Dash_Fullname).isDisplayed() }
                clickOnText(Res.string.Test_Dash_Fullname)

                checkTextAnywhereInsideWebView("2160p (4k)")

                Screengrab.screenshot("20-dash-measurement")

                Thread.sleep(3000)
                Screengrab.screenshot("5_" + locale())
            }
        }

    @Test
    fun choseWebsites() =
        runTest {
            if (!isOoni) return@runTest
            skipOnboarding()
            start()
            val websitesTitle = getOoniDescriptor(OoniTest.Websites).title()
            with(compose) {
                wait { onNodeWithContentDescription(Res.string.app_name).isDisplayed() }

                wait(timeout = 30.seconds) {
                    onNodeWithText(Res.string.Dashboard_Progress_UpdateLink_Label)
                        .isNotDisplayed()
                }

                clickOnText(Res.string.Tests_Title)
                clickOnText(websitesTitle)
                wait { onNodeWithText(websitesTitle).isDisplayed() }
                clickOnText(Res.string.Dashboard_Overview_ChooseWebsites)
                wait { onNodeWithText(Res.string.Settings_Websites_CustomURL_Title).isDisplayed() }
                Screengrab.screenshot("21-choose-websites")
                Screengrab.screenshot("6_" + locale())
            }
        }

    @Test
    fun nmsScreenshots() =
        runTest {
            if (!isNewsMediaScan) return@runTest
            skipOnboarding()
            dependencies.bootstrapTestDescriptors()
            DatabaseHelper.setup()

            val trustedDescriptor = dependencies.testDescriptorRepository
                .listLatest()
                .first()
                .first { it.id.value == "10004" }
            val trustedName = with(trustedDescriptor) { nameIntl?.getCurrent() ?: name }

            start()
            with(compose) {
                wait { onNodeWithContentDescription(Res.string.app_name).isDisplayed() }

                // Wait for description updates to finish
                Thread.sleep(2000)
                wait { onNodeWithContentDescription(Res.string.Dashboard_Progress_UpdateLink_Label).isNotDisplayed() }
                Thread.sleep(2000)

                Screengrab.screenshot("1_${locale()}")

                clickOnText(Res.string.Tests_Title)
                wait { onNodeWithText(trustedName).isDisplayed() }
                Screengrab.screenshot("2_${locale()}")

                clickOnText(Res.string.Settings_Title)

                wait { onNodeWithContentDescription(Res.string.Settings_About_Label).isDisplayed() }

                clickOnText(Res.string.Settings_About_Label)

                wait { onNodeWithTag("AboutScreen").isDisplayed() }
                Screengrab.screenshot("6_${locale()}")

                clickOnContentDescription(Res.string.Common_Back)

                clickOnText(Res.string.TestResults)

                wait { onNodeWithText(trustedName).isDisplayed() }

                Thread.sleep(3000)
                Screengrab.screenshot("3_${locale()}")

                clickOnText(trustedName)

                wait(10.seconds) { onNodeWithText("https://www.dw.com").isDisplayed() }

                // Screenshot was coming up empty, so we need to explicitly sleep here
                Thread.sleep(3000)
                Screengrab.screenshot("4_${locale()}")

                clickOnText("https://www.dw.com")

                checkTextAnywhereInsideWebView("https://www.dw.com")

                Screengrab.screenshot("5_${locale()}")
            }
        }

    private fun locale() = Screengrab.getLocale()
}
