package org.ooni.probe.uitesting

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.uitesting.helpers.checkLinkInsideWebView
import org.ooni.probe.uitesting.helpers.checkSummaryInsideWebView
import org.ooni.probe.uitesting.helpers.clickOnText
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
            with(compose) {
                clickOnText("Run")

                clickOnText("Deselect all tests")
                clickOnText("Signal Test")
                clickOnText("Run 1 test")

                wait(TEST_WAIT_TIMEOUT) {
                    onNodeWithText("Run finished. Tap to view results.").isDisplayed()
                }
                clickOnText("Run finished. Tap to view results.")

                clickOnText("Instant Messaging")
                clickOnText("Signal Test")
                wait { onNodeWithText("Measurement").isDisplayed() }
                checkSummaryInsideWebView("Signal")
            }
        }

    @Test
    fun psiphon() =
        runTest {
            with(compose) {
                clickOnText("Run")

                clickOnText("Deselect all tests")
                clickOnText("Psiphon Test")
                clickOnText("Run 1 test")

                wait(TEST_WAIT_TIMEOUT) {
                    onNodeWithText("Run finished. Tap to view results.").isDisplayed()
                }
                clickOnText("Run finished. Tap to view results.")

                clickOnText("Circumvention")
                clickOnText("Psiphon Test")
                wait { onNodeWithText("Measurement").isDisplayed() }
                checkSummaryInsideWebView("Psiphon")
            }
        }

    @Test
    fun httpHeader() =
        runTest {
            with(compose) {
                clickOnText("Run")

                clickOnText("Deselect all tests")
                onNodeWithTag("Run-DescriptorsList")
                    .performScrollToNode(hasText("HTTP Header", substring = true))
                    .performTouchInput { swipeUp() }
                clickOnText("HTTP Header", substring = true)
                clickOnText("Run 1 test")

                wait(TEST_WAIT_TIMEOUT) {
                    onNodeWithText("Run finished. Tap to view results.").isDisplayed()
                }
                clickOnText("Run finished. Tap to view results.")

                clickOnText("Performance")
                clickOnText("HTTP Header", substring = true)
                wait { onNodeWithText("Measurement").isDisplayed() }
                checkSummaryInsideWebView("middleboxes")
            }
        }

    @Test
    fun stunReachability() =
        runTest {
            with(compose) {
                clickOnText("Run")

                clickOnText("Deselect all tests")
                onNodeWithTag("Run-DescriptorsList")
                    .performScrollToNode(hasText("stunreachability"))
                    .performTouchInput { swipeUp() }
                clickOnText("stunreachability", substring = true)
                clickOnText("Run 1 test")

                wait(TEST_WAIT_TIMEOUT) {
                    onNodeWithText("Run finished. Tap to view results.").isDisplayed()
                }
                clickOnText("Run finished. Tap to view results.")

                clickOnText("Experimental")
                compose.onAllNodesWithText("stunreachability")[0].performClick()
                wait { onNodeWithText("Measurement").isDisplayed() }
                checkLinkInsideWebView("https://ooni.org/nettest/http-requests/", "STUN Reachability")
            }
        }

    companion object {
        private val TEST_WAIT_TIMEOUT = 3.minutes
    }
}
