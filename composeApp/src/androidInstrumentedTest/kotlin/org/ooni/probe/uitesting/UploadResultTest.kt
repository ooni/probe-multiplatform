package org.ooni.probe.uitesting

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_RunButton_Label
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_SelectNone
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_RunFinished
import ooniprobe.composeapp.generated.resources.Modal_ResultsNotUploaded_Uploading
import ooniprobe.composeapp.generated.resources.OONIRun_Run
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Snackbar_ResultsSomeNotUploaded_UploadAll
import ooniprobe.composeapp.generated.resources.Test_InstantMessaging_Fullname
import ooniprobe.composeapp.generated.resources.Test_Signal_Fullname
import ooniprobe.composeapp.generated.resources.measurement
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.probe.config.TestingFlags
import org.ooni.probe.data.models.SettingsKey
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
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class UploadResultTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() =
        runTest {
            TestingFlags.webviewJavascriptEnabled = true
            skipOnboarding()
            preferences.setValueByKey(SettingsKey.UPLOAD_RESULTS, false)
            start()
        }

    @Test
    @Ignore("Single test upload is currently failing on the dev back-end")
    fun uploadSingleResult() =
        runTest {
            if (!isOoni) return@runTest
            with(compose) {
                clickOnText(Res.string.OONIRun_Run)

                clickOnText(Res.string.Dashboard_RunTests_SelectNone)
                clickOnText(Res.string.Test_Signal_Fullname)
                clickOnText(getPluralString(Res.plurals.Dashboard_RunTests_RunButton_Label, 1, 1))

                clickOnText(Res.string.Dashboard_RunV2_RunFinished, timeout = TEST_WAIT_TIMEOUT)

                clickOnText(Res.string.Test_InstantMessaging_Fullname)
                clickOnText(Res.string.Snackbar_ResultsSomeNotUploaded_UploadAll)

                val onUploading =
                    onNodeWithText(getString(Res.string.Modal_ResultsNotUploaded_Uploading, 1))
                wait { onUploading.isDisplayed() }
                wait(10.seconds) { onUploading.isNotDisplayed() }

                Thread.sleep(5000)

                clickOnText(Res.string.Test_Signal_Fullname)

                wait { onNodeWithText(Res.string.measurement).isDisplayed() }
                checkSummaryInsideWebView("Signal")
            }
        }

    @Test
    @Ignore("Too long to run on Firebase Test Lab")
    fun uploadSingleResultNewsMediaScan() =
        runTest(timeout = TEST_WAIT_TIMEOUT) {
            if (!isNewsMediaScan) return@runTest
            with(compose) {
                clickOnText(Res.string.OONIRun_Run)

                clickOnText(Res.string.Dashboard_RunTests_SelectNone)
                clickOnText("Trusted International Media")
                clickOnText(getPluralString(Res.plurals.Dashboard_RunTests_RunButton_Label, 1, 1))

                clickOnText(Res.string.Dashboard_RunV2_RunFinished, timeout = TEST_WAIT_TIMEOUT)

                clickOnText("Trusted International Media")
                clickOnText(Res.string.Snackbar_ResultsSomeNotUploaded_UploadAll)

                val onUploading = onNodeWithText("Uploading", substring = true)
                wait { onUploading.isDisplayed() }
                wait(60.seconds) { onUploading.isNotDisplayed() }

                Thread.sleep(5000)

                clickOnText("https://www.dw.com")

                wait { onNodeWithText(Res.string.measurement).isDisplayed() }
                checkSummaryInsideWebView("https://www.dw.com")
            }
        }

    companion object {
        private val TEST_WAIT_TIMEOUT = 3.minutes
    }
}
