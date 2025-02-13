package org.ooni.probe.screenshots

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
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
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Title
import ooniprobe.composeapp.generated.resources.Test_Dash_Fullname
import ooniprobe.composeapp.generated.resources.Test_Performance_Fullname
import ooniprobe.composeapp.generated.resources.Test_Websites_Fullname
import ooniprobe.composeapp.generated.resources.app_name
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.getCurrent
import org.ooni.probe.uitesting.helpers.checkTextAnywhereInsideWebView
import org.ooni.probe.uitesting.helpers.clickOnContentDescription
import org.ooni.probe.uitesting.helpers.clickOnTag
import org.ooni.probe.uitesting.helpers.clickOnText
import org.ooni.probe.uitesting.helpers.defaultSettings
import org.ooni.probe.uitesting.helpers.dependencies
import org.ooni.probe.uitesting.helpers.isNewsMediaScan
import org.ooni.probe.uitesting.helpers.isOoni
import org.ooni.probe.uitesting.helpers.onNodeWithContentDescription
import org.ooni.probe.uitesting.helpers.onNodeWithText
import org.ooni.probe.uitesting.helpers.preferences
import org.ooni.probe.uitesting.helpers.skipOnboarding
import org.ooni.probe.uitesting.helpers.start
import org.ooni.probe.uitesting.helpers.wait
import org.ooni.testing.factories.MeasurementModelFactory
import org.ooni.testing.factories.ResultModelFactory
import org.ooni.testing.factories.UrlModelFactory
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

    @Before
    fun setUp() =
        runTest {
            preferences.setValueByKey(SettingsKey.SEND_CRASH, false)
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
            defaultSettings()
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

    @Test
    fun ooniResults() =
        runTest {
            if (!isOoni) return@runTest
            skipOnboarding()
            setupOoniTestResults()
            start()
            with(compose) {
                wait { onNodeWithContentDescription(Res.string.app_name).isDisplayed() }

                clickOnText(Res.string.TestResults_Overview_Title)

                wait { onNodeWithText(Res.string.Test_Websites_Fullname).isDisplayed() }

                Screengrab.screenshot("17-results")

                clickOnText(Res.string.Test_Websites_Fullname)

                wait(10.seconds) { onNodeWithText("https://z-lib.org/").isDisplayed() }

                // Screenshot was coming up empty, so we need to explicitly sleep here
                Thread.sleep(3000)
                Screengrab.screenshot("18-websites-results")

                clickOnText("https://z-lib.org/")

                checkTextAnywhereInsideWebView("https://z-lib.org/")

                Screengrab.screenshot("19-website-measurement-anomaly")

                clickOnContentDescription(Res.string.Common_Back)
                wait { onNodeWithText(Res.string.Test_Websites_Fullname).isDisplayed() }
                clickOnContentDescription(Res.string.Common_Back)
                wait { onNodeWithText(Res.string.Test_Websites_Fullname).isDisplayed() }
                clickOnText(Res.string.Test_Performance_Fullname)
                wait { onNodeWithText(Res.string.Test_Dash_Fullname).isDisplayed() }
                clickOnText(Res.string.Test_Dash_Fullname)

                checkTextAnywhereInsideWebView("2160p (4k)")

                Screengrab.screenshot("20-dash-measurement")
            }
        }

    @Test
    fun nmsResults() =
        runTest {
            if (!isNewsMediaScan) return@runTest
            skipOnboarding()
            dependencies.bootstrapTestDescriptors()
            setupNmsTestResults()

            val trustedDescriptor = dependencies.testDescriptorRepository.listLatest().first()
                .first { it.id.value == "10004" }
            val trustedName = with(trustedDescriptor) { nameIntl?.getCurrent() ?: name }

            start()
            with(compose) {
                wait { onNodeWithContentDescription(Res.string.app_name).isDisplayed() }

                clickOnText(Res.string.TestResults_Overview_Title)

                wait { onNodeWithText(trustedName).isDisplayed() }

                Screengrab.screenshot("17-results")

                clickOnText(trustedName)

                wait(10.seconds) { onNodeWithText("https://www.dw.com").isDisplayed() }

                // Screenshot was coming up empty, so we need to explicitly sleep here
                Thread.sleep(3000)
                Screengrab.screenshot("18-websites-results")

                clickOnText("https://www.dw.com")

                checkTextAnywhereInsideWebView("https://www.dw.com")

                Screengrab.screenshot("19-website-measurement")
            }
        }

    private suspend fun setupOoniTestResults() {
        dependencies.resultRepository.deleteAll()

        val networkId = dependencies.networkRepository.createIfNew(
            NetworkModel(
                networkName = "Vodafone Italia",
                asn = "AS12345",
                countryCode = "IT",
                networkType = NetworkType.Wifi,
            ),
        )

        val websitesResultId = dependencies.resultRepository.createOrUpdate(
            ResultModelFactory.build(
                id = null,
                networkId = networkId,
                descriptorName = "websites",
                isViewed = true,
                isDone = true,
                dataUsageUp = 257,
                dataUsageDown = 12345,
                taskOrigin = TaskOrigin.AutoRun,
            ),
        )
        dependencies.measurementRepository.createOrUpdate(
            MeasurementModelFactory.build(
                resultId = websitesResultId,
                test = TestType.WebConnectivity,
                urlId = dependencies.urlRepository.createOrUpdate(
                    UrlModelFactory.build(url = "https://z-lib.org/"),
                ),
                reportId = MeasurementModel.ReportId("20250210T113750Z_webconnectivity_IT_12874_n1_qx1LFyoqM4orUsor"),
                isDone = true,
                isUploaded = true,
                isAnomaly = true,
            ),
        )
        listOf(
            "https://ooni.org",
            "https://twitter.com",
            "https://facebook.com",
            "https://peta.org",
            "https://www.ran.org",
            "https://leap.se",
            "https://ilga.org",
            "https://gpgtools.org",
            "https://cdt.org",
            "https://www.viber.com",
            "https://anonymouse.org",
            "https://mail.proton.me",
            "https://kick.com",
            "https://ipfs.io",
            "https://imgur.com",
            "https://icq.com",
            "https://duckduckgo.com",
            "https://discord.com",
            "https://cloudflare-ipfs.com",
            "https://app.element.io",
            "https://github.com",
        ).forEach { url ->
            dependencies.measurementRepository.createOrUpdate(
                MeasurementModelFactory.build(
                    resultId = websitesResultId,
                    urlId = dependencies.urlRepository.createOrUpdate(
                        UrlModelFactory.build(url = url),
                    ),
                    isDone = true,
                    isUploaded = true,
                    isAnomaly = false,
                    reportId = MeasurementModel.ReportId("1234"),
                ),
            )
        }
        listOf(
            "http://mp3cool.pro",
            "https://ytx.mx",
            "https://sci-hub.se",
            "https://vibe3.com",
            "https://cb01.in",
            "http://ulub.pl",
        ).forEach { url ->
            dependencies.measurementRepository.createOrUpdate(
                MeasurementModelFactory.build(
                    resultId = websitesResultId,
                    test = TestType.WebConnectivity,
                    urlId = dependencies.urlRepository.createOrUpdate(
                        UrlModelFactory.build(url = url),
                    ),
                    isDone = true,
                    isUploaded = true,
                    isAnomaly = true,
                    reportId = MeasurementModel.ReportId("1234"),
                ),
            )
        }

        val imResultId = dependencies.resultRepository.createOrUpdate(
            ResultModelFactory.build(
                id = null,
                networkId = networkId,
                descriptorName = "instant_messaging",
                isViewed = true,
                isDone = true,
                dataUsageUp = 257,
                dataUsageDown = 12345,
                taskOrigin = TaskOrigin.AutoRun,
            ),
        )
        listOf(
            TestType.Whatsapp,
            TestType.Telegram,
            TestType.FacebookMessenger,
            TestType.Signal,
        ).forEach { testType ->
            dependencies.measurementRepository.createOrUpdate(
                MeasurementModelFactory.build(
                    resultId = imResultId,
                    test = testType,
                    isDone = true,
                    isUploaded = true,
                    reportId = MeasurementModel.ReportId("1234"),
                ),
            )
        }

        val circumventionResultId = dependencies.resultRepository.createOrUpdate(
            ResultModelFactory.build(
                id = null,
                networkId = networkId,
                descriptorName = "circumvention",
                isViewed = true,
                isDone = true,
                dataUsageUp = 257,
                dataUsageDown = 12345,
                taskOrigin = TaskOrigin.AutoRun,
            ),
        )
        listOf(
            TestType.Tor,
            TestType.Psiphon,
            TestType.HttpHeaderFieldManipulation,
            TestType.HttpInvalidRequestLine,
        ).forEach { testType ->
            dependencies.measurementRepository.createOrUpdate(
                MeasurementModelFactory.build(
                    resultId = circumventionResultId,
                    test = testType,
                    isDone = true,
                    isUploaded = true,
                    reportId = MeasurementModel.ReportId("1234"),
                ),
            )
        }

        val performanceResultId = dependencies.resultRepository.createOrUpdate(
            ResultModelFactory.build(
                id = null,
                networkId = networkId,
                descriptorName = "performance",
                isViewed = true,
                isDone = true,
                dataUsageUp = 257,
                dataUsageDown = 12345,
                taskOrigin = TaskOrigin.AutoRun,
            ),
        )
        dependencies.measurementRepository.createOrUpdate(
            MeasurementModelFactory.build(
                resultId = performanceResultId,
                test = TestType.Ndt,
                isDone = true,
                isUploaded = true,
                testKeys = """{"summary":{"upload":6058.420633995402,"download":554105.6493846333,"ping":28}}""",
                reportId = MeasurementModel.ReportId("1234"),
            ),
        )
        dependencies.measurementRepository.createOrUpdate(
            MeasurementModelFactory.build(
                resultId = performanceResultId,
                test = TestType.Dash,
                reportId = MeasurementModel.ReportId("20250210T143842Z_dash_IT_1267_n1_1hoAk1rFwFsAoyXH"),
                isDone = true,
                isUploaded = true,
                testKeys = """{"simple":{"median_bitrate":230936,"upload":1000,"download":2000}}""",
            ),
        )
    }

    private suspend fun setupNmsTestResults() {
        dependencies.resultRepository.deleteAll()

        val networkId = dependencies.networkRepository.createIfNew(
            NetworkModel(
                networkName = "Vodafone GmbH",
                asn = "AS3209",
                countryCode = "DE",
                networkType = NetworkType.Wifi,
            ),
        )

        val trustedId = dependencies.resultRepository.createOrUpdate(
            ResultModelFactory.build(
                id = null,
                networkId = networkId,
                descriptorKey = InstalledTestDescriptorModel.Key(
                    id = InstalledTestDescriptorModel.Id("10004"),
                    revision = 2,
                ),
                isViewed = true,
                isDone = true,
                dataUsageUp = 1257,
                dataUsageDown = 26589,
                taskOrigin = TaskOrigin.AutoRun,
            ),
        )
        val selectedId = dependencies.resultRepository.createOrUpdate(
            ResultModelFactory.build(
                id = null,
                networkId = networkId,
                descriptorKey = InstalledTestDescriptorModel.Key(
                    id = InstalledTestDescriptorModel.Id("10005"),
                    revision = 4,
                ),
                isViewed = true,
                isDone = true,
                dataUsageUp = 1257,
                dataUsageDown = 26589,
                taskOrigin = TaskOrigin.AutoRun,
            ),
        )
        val globalId = dependencies.resultRepository.createOrUpdate(
            ResultModelFactory.build(
                id = null,
                networkId = networkId,
                descriptorKey = InstalledTestDescriptorModel.Key(
                    id = InstalledTestDescriptorModel.Id("10006"),
                    revision = 5,
                ),
                isViewed = true,
                isDone = true,
                dataUsageUp = 1267,
                dataUsageDown = 37189,
                taskOrigin = TaskOrigin.AutoRun,
            ),
        )

        dependencies.measurementRepository.createOrUpdate(
            MeasurementModelFactory.build(
                resultId = trustedId,
                test = TestType.WebConnectivity,
                urlId = dependencies.urlRepository.createOrUpdate(
                    UrlModelFactory.build(url = "https://www.dw.com"),
                ),
                reportId = MeasurementModel.ReportId("20250205T153106Z_webconnectivity_DE_3209_n1_iB2GPLBoLLpSlEYf"),
                isDone = true,
                isUploaded = true,
            ),
        )
        listOf(
            "https://www.francemediasmonde.com/",
            "https://www.mc-doualiya.com/",
            "https://www.bbc.com/",
            "http://www.lemonde.fr/",
            "https://www.rferl.org/",
            "http://www.rfi.fr/",
            "http://www.voanews.com/",
            "https://ici.radio-canada.ca/rci/en",
            "https://www.rfa.org/english/",
            "https://www.france24.com/en/",
            "https://www3.nhk.or.jp/nhkworld/",
            "https://www.abc.net.au/news",
            "https://www.swissinfo.ch/eng/",
            "https://www.srgssr.ch/en/home/",
        ).forEach { url ->
            dependencies.measurementRepository.createOrUpdate(
                MeasurementModelFactory.build(
                    resultId = trustedId,
                    test = TestType.WebConnectivity,
                    urlId = dependencies.urlRepository.createOrUpdate(
                        UrlModelFactory.build(url = url),
                    ),
                    reportId = MeasurementModel.ReportId("12345"),
                    isDone = true,
                    isUploaded = true,
                ),
            )
        }

        repeat(91) {
            dependencies.measurementRepository.createOrUpdate(
                MeasurementModelFactory.build(
                    resultId = selectedId,
                    test = TestType.WebConnectivity,
                    urlId = dependencies.urlRepository.createOrUpdate(
                        UrlModelFactory.build(url = "https://example.org"),
                    ),
                    reportId = MeasurementModel.ReportId("12345"),
                    isDone = true,
                    isUploaded = true,
                ),
            )
        }

        repeat(142) {
            dependencies.measurementRepository.createOrUpdate(
                MeasurementModelFactory.build(
                    resultId = globalId,
                    test = TestType.WebConnectivity,
                    urlId = dependencies.urlRepository.createOrUpdate(
                        UrlModelFactory.build(url = "https://example.org"),
                    ),
                    reportId = MeasurementModel.ReportId("12345"),
                    isDone = true,
                    isUploaded = true,
                ),
            )
        }
    }
}
