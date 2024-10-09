package org.ooni.probe.testing.helpers

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.containsString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun ComposeTestRule.clickOnText(
    text: String,
    substring: Boolean = false,
): SemanticsNodeInteraction {
    wait { onNodeWithText(text, substring = substring).isDisplayed() }
    return onNodeWithText(text, substring = substring).performClick()
}

fun ComposeTestRule.clickOnContentDescription(contentDescription: String): SemanticsNodeInteraction {
    wait { onNodeWithContentDescription(contentDescription).isDisplayed() }
    return onNodeWithContentDescription(contentDescription).performClick()
}

fun ComposeTestRule.clickOnTag(tag: String): SemanticsNodeInteraction {
    wait { onNodeWithTag(tag).isDisplayed() }
    return onNodeWithTag(tag).performClick()
}

fun ComposeTestRule.wait(
    timeout: Duration = DEFAULT_WAIT_TIMEOUT,
    check: suspend () -> Boolean,
) {
    waitUntil(timeoutMillis = timeout.inWholeMilliseconds) {
        runBlocking {
            check()
        }
    }
}

fun ComposeTestRule.waitAssertion(
    timeout: Duration = DEFAULT_WAIT_TIMEOUT,
    assertion: suspend () -> Unit,
) {
    wait(timeout) {
        try {
            assertion()
            true
        } catch (e: AssertionError) {
            false
        }
    }
}

private val DEFAULT_WAIT_TIMEOUT = 3.seconds // Emulator can be slow on CI

fun ComposeTestRule.checkSummaryInsideWebView(text: String) {
    waitAssertion(WEBSITE_WAIT_TIMEOUT) {
        onWebView()
            .withElement(
                findElement(Locator.CSS_SELECTOR, "*[data-test-id=\"common-summary\"]"),
            )
            .check(webMatches(getText(), containsString(text)))
    }
}

fun ComposeTestRule.checkLinkInsideWebView(
    link: String,
    text: String,
) {
    waitAssertion(WEBSITE_WAIT_TIMEOUT) {
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "a[href=\"$link\""))
            .check(webMatches(getText(), containsString(text)))
    }
}

private val WEBSITE_WAIT_TIMEOUT = 10.seconds
