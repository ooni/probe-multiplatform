package org.ooni.probe.uitesting

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import ooniprobe.composeapp.generated.resources.Common_Expand
import ooniprobe.composeapp.generated.resources.Dashboard_LastResults_SeeResults
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_RunButton_Label
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_SelectNone
import ooniprobe.composeapp.generated.resources.Measurement_Title
import ooniprobe.composeapp.generated.resources.OONIRun_Run
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Test_Circumvention_Fullname
import ooniprobe.composeapp.generated.resources.Test_Experimental_Fullname
import ooniprobe.composeapp.generated.resources.Test_InstantMessaging_Fullname
import ooniprobe.composeapp.generated.resources.Test_Performance_Fullname
import ooniprobe.composeapp.generated.resources.Test_Psiphon_Fullname
import ooniprobe.composeapp.generated.resources.Test_Signal_Fullname
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.uitesting.helpers.checkSummaryInsideWebView
import org.ooni.probe.uitesting.helpers.checkTextAnywhereInsideWebView
import org.ooni.probe.uitesting.helpers.clickOnContentDescription
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
class RunningTestsTest {
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
                clickOnContentDescription(getString(Res.string.Common_Expand) + " " + getString(Res.string.Test_InstantMessaging_Fullname))
                clickOnText(Res.string.Test_Signal_Fullname)
                clickOnRunButton(1)

                clickOnText(Res.string.Dashboard_LastResults_SeeResults, timeout = TEST_WAIT_TIMEOUT)

                clickOnText(Res.string.Test_InstantMessaging_Fullname)
                clickOnText(Res.string.Test_Signal_Fullname)
                wait { onNodeWithText(Res.string.Measurement_Title).isDisplayed() }
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
                clickOnContentDescription(getString(Res.string.Common_Expand) + " " + getString(Res.string.Test_Circumvention_Fullname))
                clickOnText(Res.string.Test_Psiphon_Fullname)
                clickOnRunButton(1)

                clickOnText(Res.string.Dashboard_LastResults_SeeResults, timeout = TEST_WAIT_TIMEOUT)

                clickOnText(Res.string.Test_Circumvention_Fullname)
                clickOnText(Res.string.Test_Psiphon_Fullname)
                wait { onNodeWithText(Res.string.Measurement_Title).isDisplayed() }
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
                clickOnContentDescription(getString(Res.string.Common_Expand) + " " + getString(Res.string.Test_Performance_Fullname))
                clickOnText("HTTP Header", substring = true)
                clickOnRunButton(1)

                clickOnText(Res.string.Dashboard_LastResults_SeeResults, timeout = TEST_WAIT_TIMEOUT)

                clickOnText(Res.string.Test_Performance_Fullname)
                clickOnText("HTTP Header", substring = true)
                wait { onNodeWithText(Res.string.Measurement_Title).isDisplayed() }
                checkSummaryInsideWebView("middleboxes")
            }
        }

    @Test
    fun stunReachability() =
        runTest(timeout = TEST_WAIT_TIMEOUT) {
            if (!isOoni) return@runTest
            with(compose) {
                clickOnText(Res.string.OONIRun_Run)

                clickOnText(Res.string.Dashboard_RunTests_SelectNone)
                clickOnContentDescription(getString(Res.string.Common_Expand) + " " + getString(Res.string.Test_Experimental_Fullname))
                clickOnText("stunreachability", substring = true)
                clickOnRunButton(1)

                clickOnText(Res.string.Dashboard_LastResults_SeeResults, timeout = TEST_WAIT_TIMEOUT)

                clickOnText(Res.string.Test_Experimental_Fullname)
                compose.onAllNodesWithText("stunreachability")[0].performClick()
                wait { onNodeWithText(Res.string.Measurement_Title).isDisplayed() }
                checkTextAnywhereInsideWebView("stunreachability")
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
                clickOnRunButton(1)

                clickOnText(Res.string.Dashboard_LastResults_SeeResults, timeout = TEST_WAIT_TIMEOUT)

                clickOnText("Trusted International Media")
                clickOnText("https://www.dw.com")
                wait { onNodeWithText(Res.string.Measurement_Title).isDisplayed() }
                checkSummaryInsideWebView("https://www.dw.com")
            }
        }

    private suspend fun ComposeTestRule.clickOnRunButton(quantity: Int) {
        clickOnText(
            getPluralString(
                Res.plurals.Dashboard_RunTests_RunButton_Label,
                quantity,
                quantity,
            ),
        )
    }

    companion object {
        private val TEST_WAIT_TIMEOUT = 3.minutes
    }
}
