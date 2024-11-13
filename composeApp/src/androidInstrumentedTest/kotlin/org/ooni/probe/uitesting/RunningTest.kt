package org.ooni.probe.uitesting

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_RunButton_Label
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_SelectNone
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_RunFinished
import ooniprobe.composeapp.generated.resources.OONIRun_Run
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Test_Circumvention_Fullname
import ooniprobe.composeapp.generated.resources.Test_Experimental_Fullname
import ooniprobe.composeapp.generated.resources.Test_InstantMessaging_Fullname
import ooniprobe.composeapp.generated.resources.Test_Performance_Fullname
import ooniprobe.composeapp.generated.resources.Test_Psiphon_Fullname
import ooniprobe.composeapp.generated.resources.Test_Signal_Fullname
import ooniprobe.composeapp.generated.resources.measurement
import org.jetbrains.compose.resources.getPluralString
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.uitesting.helpers.checkLinkInsideWebView
import org.ooni.probe.uitesting.helpers.checkSummaryInsideWebView
import org.ooni.probe.uitesting.helpers.clickOnText
import org.ooni.probe.uitesting.helpers.isNewsMediaScan
import org.ooni.probe.uitesting.helpers.isOoni
import org.ooni.probe.uitesting.helpers.onNodeWithText
import org.ooni.probe.uitesting.helpers.preferences
import org.ooni.probe.uitesting.helpers.skipOnboarding
import org.ooni.probe.uitesting.helpers.start
import org.ooni.probe.uitesting.helpers.wait
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
class RunningTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() =
        runTest {
            skipOnboarding()
            preferences.setValueByKey(SettingsKey.UPLOAD_RESULTS, true)
            start()
        }

    @Test
    fun signal() =
        runTest {
            if (!isOoni) return@runTest
            with(compose) {
                clickOnText(Res.string.OONIRun_Run)

                clickOnText(Res.string.Dashboard_RunTests_SelectNone)
                clickOnText(Res.string.Test_Signal_Fullname)
                clickOnText(getPluralString(Res.plurals.Dashboard_RunTests_RunButton_Label, 1, 1))

                clickOnText(Res.string.Dashboard_RunV2_RunFinished, timeout = TEST_WAIT_TIMEOUT)

                clickOnText(Res.string.Test_InstantMessaging_Fullname)
                clickOnText(Res.string.Test_Signal_Fullname)
                wait { onNodeWithText(Res.string.measurement).isDisplayed() }
                checkSummaryInsideWebView("Signal")
            }
        }

    @Test
    fun psiphon() =
        runTest {
            if (!isOoni) return@runTest
            with(compose) {
                clickOnText(Res.string.OONIRun_Run)

                clickOnText(Res.string.Dashboard_RunTests_SelectNone)
                clickOnText(Res.string.Test_Psiphon_Fullname)
                clickOnText(getPluralString(Res.plurals.Dashboard_RunTests_RunButton_Label, 1, 1))

                clickOnText(Res.string.Dashboard_RunV2_RunFinished, timeout = TEST_WAIT_TIMEOUT)

                clickOnText(Res.string.Test_Circumvention_Fullname)
                clickOnText(Res.string.Test_Psiphon_Fullname)
                wait { onNodeWithText(Res.string.measurement).isDisplayed() }
                checkSummaryInsideWebView("Psiphon")
            }
        }

    @Test
    fun httpHeader() =
        runTest {
            if (!isOoni) return@runTest
            with(compose) {
                clickOnText(Res.string.OONIRun_Run)

                clickOnText(Res.string.Dashboard_RunTests_SelectNone)
                onNodeWithTag("Run-DescriptorsList")
                    .performScrollToNode(hasText("HTTP Header", substring = true))
                    .performTouchInput { swipeUp() }
                clickOnText("HTTP Header", substring = true)
                clickOnText(getPluralString(Res.plurals.Dashboard_RunTests_RunButton_Label, 1, 1))

                clickOnText(Res.string.Dashboard_RunV2_RunFinished, timeout = TEST_WAIT_TIMEOUT)

                clickOnText(Res.string.Test_Performance_Fullname)
                clickOnText("HTTP Header", substring = true)
                wait { onNodeWithText(Res.string.measurement).isDisplayed() }
                checkSummaryInsideWebView("middleboxes")
            }
        }

    @Test
    fun stunReachability() =
        runTest {
            if (!isOoni) return@runTest
            with(compose) {
                clickOnText(Res.string.OONIRun_Run)

                clickOnText(Res.string.Dashboard_RunTests_SelectNone)
                onNodeWithTag("Run-DescriptorsList")
                    .performScrollToNode(hasText("stunreachability"))
                    .performTouchInput { swipeUp() }
                clickOnText("stunreachability", substring = true)
                clickOnText(getPluralString(Res.plurals.Dashboard_RunTests_RunButton_Label, 1, 1))

                clickOnText(Res.string.Dashboard_RunV2_RunFinished, timeout = TEST_WAIT_TIMEOUT)

                clickOnText(Res.string.Test_Experimental_Fullname)
                compose.onAllNodesWithText("stunreachability")[0].performClick()
                wait { onNodeWithText(Res.string.measurement).isDisplayed() }
                checkLinkInsideWebView(
                    "https://ooni.org/nettest/http-requests/",
                    "STUN Reachability",
                )
            }
        }

    @Test
    @Ignore("Too long to run on Firebase Test Lab")
    fun trustedInternationalMedia() =
        runTest(timeout = TEST_WAIT_TIMEOUT) {
            if (!isNewsMediaScan) return@runTest
            with(compose) {
                clickOnText(Res.string.OONIRun_Run)

                clickOnText(Res.string.Dashboard_RunTests_SelectNone)
                clickOnText("Trusted International Media")
                clickOnText(getPluralString(Res.plurals.Dashboard_RunTests_RunButton_Label, 1, 1))

                clickOnText(Res.string.Dashboard_RunV2_RunFinished, timeout = TEST_WAIT_TIMEOUT)

                clickOnText("Trusted International Media")
                clickOnText("https://www.dw.com")
                wait { onNodeWithText(Res.string.measurement).isDisplayed() }
                checkSummaryInsideWebView("https://www.dw.com")
            }
        }

    companion object {
        private val TEST_WAIT_TIMEOUT = 3.minutes
    }
}
