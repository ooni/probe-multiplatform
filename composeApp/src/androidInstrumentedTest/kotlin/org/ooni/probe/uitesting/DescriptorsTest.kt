package org.ooni.probe.uitesting

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ooniprobe.composeapp.generated.resources.AddDescriptor_AutoUpdate
import ooniprobe.composeapp.generated.resources.AddDescriptor_Title
import ooniprobe.composeapp.generated.resources.Dashboard_Progress_ReviewLink_Action
import ooniprobe.composeapp.generated.resources.Dashboard_ReviewDescriptor_Button_Last
import ooniprobe.composeapp.generated.resources.Dashboard_Runv2_Overview_UninstallLink
import ooniprobe.composeapp.generated.resources.AddDescriptor_InstallForLater
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Test_WebConnectivity_Fullname
import ooniprobe.composeapp.generated.resources.Tests_Title
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.TestOonimkallBridge
import org.ooni.probe.MainActivity
import org.ooni.probe.uitesting.helpers.clickOnText
import org.ooni.probe.uitesting.helpers.context
import org.ooni.probe.uitesting.helpers.dependencies
import org.ooni.probe.uitesting.helpers.disableRefreshArticles
import org.ooni.probe.uitesting.helpers.isNewsMediaScan
import org.ooni.probe.uitesting.helpers.onAllNodesWithText
import org.ooni.probe.uitesting.helpers.onNodeWithText
import org.ooni.probe.uitesting.helpers.preferences
import org.ooni.probe.uitesting.helpers.skipOnboarding
import org.ooni.probe.uitesting.helpers.start
import org.ooni.probe.uitesting.helpers.wait
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class DescriptorsTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() =
        runTest {
            skipOnboarding()
            disableRefreshArticles()
        }

    @Test
    fun installAndUninstall() {
        runTest {
            if (isNewsMediaScan) return@runTest

            start(
                Intent(context, MainActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://run.test.ooni.org/v2/10460")),
            )

            with(compose) {
                wait(DESCRIPTOR_DOWNLOAD_WAIT_TIMEOUT) {
                    onNodeWithText(Res.string.AddDescriptor_Title).isDisplayed()
                }

                onNodeWithText("Testing").assertIsDisplayed()
                onNodeWithText("Android instrumented tests").assertIsDisplayed()
                onNodeWithText(Res.string.Test_WebConnectivity_Fullname).assertIsDisplayed()

                clickOnText(Res.string.AddDescriptor_InstallForLater)

                Thread.sleep(2000)

                clickOnText(Res.string.Tests_Title)
                wait { onNodeWithTag("Descriptors-List").isDisplayed() }
                onNodeWithTag("Descriptors-List")
                    .performScrollToNode(hasText("Android instrumented tests"))
                onNodeWithText("Testing").assertIsDisplayed()

                val descriptor = dependencies.getTestDescriptors
                    .latest()
                    .first()
                    .first()
                assertEquals("Testing", descriptor.name)

                val test = descriptor.netTests.first()
                assertTrue(preferences.isNetTestEnabled(descriptor, test, isAutoRun = true).first())

                clickOnText("Android instrumented tests")
                onNodeWithText(getString(Res.string.Dashboard_Runv2_Overview_UninstallLink))
                    .performScrollTo()
                clickOnText(Res.string.Dashboard_Runv2_Overview_UninstallLink)

                onAllNodesWithText(Res.string.Dashboard_Runv2_Overview_UninstallLink)
                    .onLast()
                    .performClick()

                onNodeWithText("Testing").assertIsNotDisplayed()
            }
        }
    }

    @Test
    fun installAndUpdate() =
        runTest {
            if (isNewsMediaScan) return@runTest

            start(
                Intent(context, MainActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .setData(Uri.parse(DESCRIPTOR_URL)),
            )

            with(compose) {
                wait(DESCRIPTOR_DOWNLOAD_WAIT_TIMEOUT) {
                    onNodeWithText(Res.string.AddDescriptor_Title).isDisplayed()
                }
                clickOnText(Res.string.AddDescriptor_AutoUpdate)
                clickOnText(Res.string.AddDescriptor_InstallForLater)

                Thread.sleep(2000)

                setupTestEngine()

                clickOnText(Res.string.Tests_Title)
                wait { onNodeWithTag("Descriptors-List").isDisplayed() }
                // Pull down to refresh
                onNodeWithTag("Descriptors-List").performTouchInput { swipeDown() }

                clickOnText(
                    Res.string.Dashboard_Progress_ReviewLink_Action,
                    timeout = DESCRIPTOR_DOWNLOAD_WAIT_TIMEOUT,
                )

                wait { onNodeWithText("Testing 2").isDisplayed() }

                clickOnText(getString(Res.string.Dashboard_ReviewDescriptor_Button_Last, 1, 1))

                onNodeWithTag("Descriptors-List")
                    .performScrollToNode(hasText("Android instrumented tests"))
                onNodeWithText("Testing 2").assertIsDisplayed()
            }
        }

    private fun setupTestEngine() {
        val testBridge = TestOonimkallBridge()
        dependencies.engine.bridge = testBridge
        testBridge.httpDoMock = {
            OonimkallBridge.HTTPResponse(
                body = when (it.url) {
                    "https://api.dev.ooni.io/api/v2/oonirun/links/10460" ->
                        UPDATED_DESCRIPTOR_JSON

                    "https://api.dev.ooni.io/api/v2/oonirun/links/10460/revisions" ->
                        DESCRIPTOR_REVISIONS_JSON

                    else ->
                        throw IllegalStateException("Response not mocked for ${it.url}")
                },
            )
        }
    }

    companion object {
        private val DESCRIPTOR_DOWNLOAD_WAIT_TIMEOUT = 10.seconds
        private const val DESCRIPTOR_URL = "https://run.test.ooni.org/v2/10460"
        private val UPDATED_DESCRIPTOR_JSON = """
            {
               "name":"Testing 2",
               "short_description":"Android instrumented tests",
               "description":"This is OONI Run Link for the Android instrumented tests",
               "author":"sergio@bloco.io",
               "nettests":[
                  {
                     "test_name":"web_connectivity",
                     "inputs":[
                        "https://example.org"
                     ],
                     "options":{},
                     "backend_options":{},
                     "is_background_run_enabled_default":false,
                     "is_manual_run_enabled_default":false
                  }
               ],
               "name_intl":{},
               "short_description_intl":{},
               "description_intl":{},
               "icon":"FaCube",
               "color":"#73d8ff",
               "expiration_date":"2100-12-31T00:00:00.000000Z",
               "oonirun_link_id":"10460",
               "date_created":"2024-10-09T10:53:52.000000Z",
               "date_updated":"2024-10-09T17:00:00.000000Z",
               "revision":"2",
               "is_mine":false,
               "is_expired":false
            }
        """.trimIndent()
        private val DESCRIPTOR_REVISIONS_JSON = """
            {"revisions":["1"]}
        """.trimIndent()
    }
}
