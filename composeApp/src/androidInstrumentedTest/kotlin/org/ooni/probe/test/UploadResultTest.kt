package org.ooni.probe.test

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.test.helpers.CleanTestRule
import org.ooni.probe.test.helpers.FlakyTestRule
import org.ooni.probe.test.helpers.checkSummaryInsideWebView
import org.ooni.probe.test.helpers.clickOnText
import org.ooni.probe.test.helpers.preferences
import org.ooni.probe.test.helpers.skipOnboarding
import org.ooni.probe.test.helpers.start
import org.ooni.probe.test.helpers.wait
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class UploadResultTest {
    @get:Rule
    val clean = CleanTestRule()

    @get:Rule
    val flakyTestRule = FlakyTestRule()

    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() =
        runTest {
            skipOnboarding()
            preferences.setValueByKey(SettingsKey.UPLOAD_RESULTS, false)
            start()
        }

    @Test
    fun uploadSingleResult() =
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
                clickOnText("Upload All")

                wait {
                    onNodeWithText("Uploading", substring = true).isDisplayed()
                }
                waitUntil(10.seconds.inWholeMilliseconds) {
                    onNodeWithText("Uploading", substring = true).isNotDisplayed()
                }

                Thread.sleep(10000)

                clickOnText("Signal Test")

                wait { onNodeWithText("Measurement").isDisplayed() }
                checkSummaryInsideWebView("Signal")
            }
        }

    companion object {
        private val TEST_WAIT_TIMEOUT = 1.minutes
    }
}
