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
import ooniprobe.composeapp.generated.resources.AddDescriptor_InstallForLater
import ooniprobe.composeapp.generated.resources.AddDescriptor_Title
import ooniprobe.composeapp.generated.resources.Dashboard_Progress_ReviewLink_Action
import ooniprobe.composeapp.generated.resources.Dashboard_ReviewDescriptor_Button_Last
import ooniprobe.composeapp.generated.resources.Dashboard_Runv2_Overview_UninstallLink
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
import org.ooni.probe.MainActivity
import org.ooni.probe.uitesting.helpers.TestFixtures
import org.ooni.probe.uitesting.helpers.clickOnText
import org.ooni.probe.uitesting.helpers.context
import org.ooni.probe.uitesting.helpers.dependencies
import org.ooni.probe.uitesting.helpers.disableRefreshArticles
import org.ooni.probe.uitesting.helpers.isNewsMediaScan
import org.ooni.probe.uitesting.helpers.onAllNodesWithText
import org.ooni.probe.uitesting.helpers.onNodeWithText
import org.ooni.probe.uitesting.helpers.preferences
import org.ooni.probe.uitesting.helpers.setupMockedEngine
import org.ooni.probe.uitesting.helpers.skipOnboarding
import org.ooni.probe.uitesting.helpers.start
import org.ooni.probe.uitesting.helpers.wait
import org.ooni.probe.uitesting.helpers.waitAssertion
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class DescriptorsTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private var descriptorUpdatePublished = false

    @Before
    fun setUp() =
        runTest {
            skipOnboarding()
            disableRefreshArticles()
            installDescriptorEngine()
        }

    /**
     * Serves the OONI Run descriptor offline. Before [publishDescriptorUpdate]
     * the link returns revision 1 ("Testing"); afterwards revision 2
     * ("Testing 2"), driving the auto-update flow without any network.
     */
    private fun installDescriptorEngine() {
        descriptorUpdatePublished = false
        setupMockedEngine {
            httpDo = { request ->
                val body = when {
                    request.url.endsWith(TestFixtures.DESCRIPTOR_REVISIONS_PATH) ->
                        TestFixtures.DESCRIPTOR_REVISIONS_JSON

                    request.url.endsWith(TestFixtures.DESCRIPTOR_LINK_PATH) ->
                        if (descriptorUpdatePublished) {
                            TestFixtures.UPDATED_DESCRIPTOR_JSON
                        } else {
                            TestFixtures.ORIGINAL_DESCRIPTOR_JSON
                        }

                    else ->
                        throw IllegalStateException("Response not mocked for ${request.url}")
                }
                OonimkallBridge.HTTPResponse(body = body)
            }
        }
    }

    private fun publishDescriptorUpdate() {
        descriptorUpdatePublished = true
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

                waitAssertion {
                    onNodeWithText(Res.string.AddDescriptor_Title).assertIsNotDisplayed()
                }

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

                waitAssertion {
                    onNodeWithText("Testing").assertIsNotDisplayed()
                }
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

                waitAssertion {
                    onNodeWithText(Res.string.AddDescriptor_Title).assertIsNotDisplayed()
                }

                // Publish revision 2 so the pull-to-refresh below finds an update.
                publishDescriptorUpdate()

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

    companion object {
        private val DESCRIPTOR_DOWNLOAD_WAIT_TIMEOUT = 10.seconds
        private val DESCRIPTOR_URL = TestFixtures.DESCRIPTOR_URL
    }
}
