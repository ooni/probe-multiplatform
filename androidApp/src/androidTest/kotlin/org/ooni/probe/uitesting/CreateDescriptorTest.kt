package org.ooni.probe.uitesting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Tests_Title
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ooni.engine.models.Success
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.uitesting.helpers.clickOnContentDescription
import org.ooni.probe.uitesting.helpers.clickOnTag
import org.ooni.probe.uitesting.helpers.clickOnText
import org.ooni.probe.uitesting.helpers.dependencies
import org.ooni.probe.uitesting.helpers.disableRefreshArticles
import org.ooni.probe.uitesting.helpers.setupMockedEngine
import org.ooni.probe.uitesting.helpers.skipOnboarding
import org.ooni.probe.uitesting.helpers.start
import org.ooni.probe.uitesting.helpers.wait
import kotlin.time.Duration.Companion.seconds

/**
 * Drives the "Create Run Link" flow (login -> paste token -> author a link -> success) entirely
 * offline: the `ooniauth`/`oonirun` POST traffic is served through the `passportPost` fake below,
 * the same seam [org.ooni.probe.uitesting.helpers] already uses for GET traffic.
 */
@RunWith(AndroidJUnit4::class)
class CreateDescriptorTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() =
        runTest {
            skipOnboarding()
            disableRefreshArticles()
            setupMockedEngine()
            installNetworkFixtures()
        }

    private fun installNetworkFixtures() {
        dependencies.passportPost = { url, _, _, _, _ ->
            when {
                url.endsWith("/api/v2/ooniauth/user-login") ->
                    Success(PassportHttpResponse(200, "HTTP/1.1", emptyList(), LOGIN_RESPONSE_JSON))

                url.endsWith("/api/v2/ooniauth/user-session") ->
                    Success(PassportHttpResponse(200, "HTTP/1.1", emptyList(), SESSION_RESPONSE_JSON))

                url.endsWith("/api/v2/oonirun/links") ->
                    Success(PassportHttpResponse(200, "HTTP/1.1", emptyList(), CREATED_DESCRIPTOR_JSON))

                else -> throw IllegalStateException("Response not mocked for $url")
            }
        }
    }

    @Test
    fun createsANewRunLink() =
        runTest {
            start()

            with(compose) {
                clickOnText(Res.string.Tests_Title)
                clickOnContentDescription(Res.string.CreateDescriptor_Title)

                wait { onNodeWithTag("CreateDescriptor-Email").isDisplayed() }
                onNodeWithTag("CreateDescriptor-Email").performTextReplacement(EMAIL_ADDRESS)
                clickOnTag("CreateDescriptor-SendLoginLink")

                wait { onNodeWithTag("CreateDescriptor-Token").isDisplayed() }
                onNodeWithTag("CreateDescriptor-Token")
                    .performTextReplacement("https://run.ooni.org/login?token=$LOGIN_TOKEN")
                clickOnTag("CreateDescriptor-Verify")

                wait { onNodeWithTag("CreateDescriptor-Name").isDisplayed() }
                onNodeWithTag("CreateDescriptor-Name").performTextReplacement("My censorship watchlist")
                onNodeWithTag("CreateDescriptor-ShortDescription").performTextReplacement("Websites to keep an eye on")
                onNodeWithTag("CreateDescriptor-Description").performTextReplacement("A short description of the link.")
                onNodeWithTag("CreateDescriptor-Inputs-0").performTextReplacement("https://ooni.org")

                clickOnTag("CreateDescriptor-Submit")

                wait(SUBMIT_WAIT_TIMEOUT) { onNodeWithTag("CreateDescriptor-RunLink").isDisplayed() }
                // The dashboard domain differs between build types (e.g. run.test.ooni.org in
                // debug) - match on the link id instead of hardcoding the full URL.
                onNodeWithText("/v2/$CREATED_LINK_ID", substring = true).assertIsDisplayed()
                onNodeWithText("ooni://runv2/$CREATED_LINK_ID").assertIsDisplayed()
            }
        }

    companion object {
        private const val EMAIL_ADDRESS = "test@example.org"
        private const val LOGIN_TOKEN = "abc123"
        private const val CREATED_LINK_ID = "9999"
        private val SUBMIT_WAIT_TIMEOUT = 10.seconds

        private val LOGIN_RESPONSE_JSON = """
            {
                "email_address": "$EMAIL_ADDRESS",
                "login_token_expiration": "2030-01-01T00:00:00Z"
            }
        """.trimIndent()

        private val SESSION_RESPONSE_JSON = """
            {
                "session_token": "jwt-instrumented-test",
                "email_address": "$EMAIL_ADDRESS",
                "role": "user",
                "login_time": "2025-01-01T00:00:00Z",
                "is_logged_in": true
            }
        """.trimIndent()

        private val CREATED_DESCRIPTOR_JSON = """
            {
                "oonirun_link_id": "$CREATED_LINK_ID",
                "name": "My censorship watchlist",
                "short_description": "Websites to keep an eye on",
                "description": "A short description of the link.",
                "author": "$EMAIL_ADDRESS",
                "nettests": [{"test_name": "web_connectivity", "inputs": ["https://ooni.org"]}],
                "name_intl": null,
                "short_description_intl": null,
                "description_intl": null,
                "icon": null,
                "color": null,
                "expiration_date": "2030-01-01T00:00:00Z",
                "date_created": "2025-01-01T00:00:00Z",
                "date_updated": "2025-01-01T00:00:00Z",
                "is_expired": false,
                "revision": "1"
            }
        """.trimIndent()
    }
}
