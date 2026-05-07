package org.ooni.probe.screenshots

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal val DEFAULT_WAIT: Duration = 8.seconds
internal val LONG_WAIT: Duration = 30.seconds

internal fun string(resource: StringResource): String = runBlocking { getString(resource) }

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.waitForText(
    text: String,
    substring: Boolean = false,
    timeout: Duration = DEFAULT_WAIT,
) {
    waitUntil(timeoutMillis = timeout.inWholeMilliseconds) {
        onAllNodesWithText(text, substring = substring)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.waitForContentDescription(
    description: String,
    timeout: Duration = DEFAULT_WAIT,
) {
    waitUntil(timeoutMillis = timeout.inWholeMilliseconds) {
        onAllNodesWithContentDescription(description)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.waitForTag(
    tag: String,
    timeout: Duration = DEFAULT_WAIT,
) {
    waitUntil(timeoutMillis = timeout.inWholeMilliseconds) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.clickText(
    text: String,
    substring: Boolean = false,
    timeout: Duration = DEFAULT_WAIT,
) {
    waitForText(text, substring = substring, timeout = timeout)
    runOnUiThread {
        onAllNodesWithText(text, substring = substring).onFirst().performClick()
    }
    waitForIdle()
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.clickContentDescription(
    description: String,
    timeout: Duration = DEFAULT_WAIT,
) {
    waitForContentDescription(description, timeout = timeout)
    runOnUiThread {
        onAllNodesWithContentDescription(description).onFirst().performClick()
    }
    waitForIdle()
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.clickTag(
    tag: String,
    timeout: Duration = DEFAULT_WAIT,
) {
    waitForTag(tag, timeout = timeout)
    runOnUiThread {
        onAllNodesWithTag(tag).onFirst().performClick()
    }
    waitForIdle()
}
