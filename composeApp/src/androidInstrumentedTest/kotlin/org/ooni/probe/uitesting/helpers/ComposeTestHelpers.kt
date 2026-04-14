package org.ooni.probe.uitesting.helpers

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.model.Atoms.getCurrentUrl
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import co.touchlab.kermit.Logger
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.containsString
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun ComposeTestRule.clickOnText(
    stringRes: StringResource,
    timeout: Duration = DEFAULT_WAIT_TIMEOUT,
): SemanticsNodeInteraction = clickOnText(getString(stringRes), timeout = timeout)

fun ComposeTestRule.clickOnText(
    text: String,
    substring: Boolean = false,
    timeout: Duration = DEFAULT_WAIT_TIMEOUT,
): SemanticsNodeInteraction {
    wait(timeout) { onAllNodesWithText(text, substring = substring).onFirst().isDisplayed() }
    return onAllNodesWithText(text, substring = substring).onFirst().performClick()
}

suspend fun ComposeTestRule.clickOnContentDescription(stringRes: StringResource) = clickOnContentDescription(getString(stringRes))

fun ComposeTestRule.clickOnContentDescription(contentDescription: String): SemanticsNodeInteraction {
    wait { onNodeWithContentDescription(contentDescription).isDisplayed() }
    return onNodeWithContentDescription(contentDescription).performClick()
}

fun ComposeTestRule.clickOnTag(tag: String): SemanticsNodeInteraction {
    wait { onNodeWithTag(tag).isDisplayed() }
    return onNodeWithTag(tag).performClick()
}

suspend fun ComposeTestRule.onNodeWithText(stringRes: StringResource) = onNodeWithText(getString(stringRes))

suspend fun ComposeTestRule.onAllNodesWithText(stringRes: StringResource) = onAllNodesWithText(getString(stringRes))

suspend fun ComposeTestRule.onNodeWithContentDescription(stringRes: StringResource) = onNodeWithContentDescription(getString(stringRes))

fun ComposeTestRule.wait(
    timeout: Duration = DEFAULT_WAIT_TIMEOUT,
    isOptional: Boolean = false,
    check: suspend () -> Boolean,
) {
    try {
        waitUntil(timeoutMillis = timeout.inWholeMilliseconds) {
            runBlocking {
                check()
            }
        }
    } catch (e: Throwable) {
        if (isOptional) {
            Logger.w("Wait failed but it was optional", e)
        } else {
            throw e
        }
    }
}

fun ComposeTestRule.waitAssertion(
    timeout: Duration = DEFAULT_WAIT_TIMEOUT,
    isOptional: Boolean = false,
    assertion: suspend () -> Unit,
) {
    wait(timeout, isOptional = isOptional) {
        try {
            assertion()
            true
        } catch (e: Throwable) {
            Logger.w("waitAssertion failure", e)
            false
        }
    }
}

private val DEFAULT_WAIT_TIMEOUT = 3.seconds // Emulator can be slow on CI

fun ComposeTestRule.checkUrlInsideWebView(
    text: String,
    isOptional: Boolean = false,
) {
    waitAssertion(WEBSITE_WAIT_TIMEOUT, isOptional = isOptional) {
        onWebView()
            .check(webMatches(getCurrentUrl(), containsString(text)))
    }
}

fun ComposeTestRule.checkSummaryInsideWebView(
    text: String,
    isOptional: Boolean = false,
) {
    waitAssertion(WEBSITE_WAIT_TIMEOUT, isOptional = isOptional) {
        onWebView()
            .withElement(
                findElement(Locator.CSS_SELECTOR, "*[data-testid=\"common-summary\"]"),
            ).check(webMatches(getText(), containsString(text)))
    }
}

fun ComposeTestRule.checkTextAnywhereInsideWebView(
    text: String,
    isOptional: Boolean = false,
) {
    waitAssertion(WEBSITE_WAIT_TIMEOUT, isOptional = isOptional) {
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "*"))
            .check(webMatches(getText(), containsString(text)))
    }
}

private val WEBSITE_WAIT_TIMEOUT = 20.seconds
