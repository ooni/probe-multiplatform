package org.ooni.probe.ui.choosewebsites

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.flow.flowOf
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.RunSpecification
import kotlin.test.Test
import kotlin.test.assertEquals

class ChooseWebsitesTest {
    private var runSpec: RunSpecification? = null

    @Test
    fun addWebsites() =
        runComposeUiTest {
            val viewModel = buildViewModel()
            setContent {
                val state by viewModel.state.collectAsState()
                ChooseWebsitesScreen(state, viewModel::onEvent)
            }

            val websites = listOf(
                "http://website1.com",
                "http://website2.com",
                "http://website3.com",
            )

            repeat(websites.size - 1) {
                onNodeWithText("Add website")
                    .performScrollTo()
                    .performClick()
            }
            websites.forEachIndexed { index, url ->
                onAllNodesWithTag("ChooseWebsites-UrlField")[index]
                    .performTextReplacement(url)
            }
            onNodeWithText("Test ${websites.size} URLs").performClick()

            waitUntil { runSpec != null }
            val spec = runSpec as RunSpecification.Full
            assertEquals(false, spec.isRerun)
            assertEquals(TaskOrigin.OoniRun, spec.taskOrigin)
            assertEquals(1, spec.tests.size)
            assertEquals(
                TestType.WebConnectivity,
                spec.tests
                    .first()
                    .netTests
                    .first()
                    .test,
            )
            assertEquals(
                websites,
                spec.tests
                    .first()
                    .netTests
                    .first()
                    .inputs,
            )
        }

    @Test
    fun websiteLimit() =
        runComposeUiTest {
            val viewModel = buildViewModel()
            setContent {
                val state by viewModel.state.collectAsState()
                ChooseWebsitesScreen(state, viewModel::onEvent)
            }

            ChooseWebsitesViewModel.maxWebsites = 3
            // We already start with 1 website so -1
            repeat(ChooseWebsitesViewModel.maxWebsites - 1) {
                onNodeWithText("Add website")
                    .performScrollTo()
                    .performClick()
            }

            onNodeWithTag("ChooseWebsites-List")
                .performScrollToNode(hasText("Add website"))
            onNodeWithText("Add website")
                .assertIsNotEnabled()
        }

    @Test
    fun initialUrl() =
        runComposeUiTest {
            val viewModel = buildViewModel(initialUrl = "https://ooni.org")
            setContent {
                val state by viewModel.state.collectAsState()
                ChooseWebsitesScreen(state, viewModel::onEvent)
            }

            onNodeWithText("https://ooni.org").assertIsDisplayed()
        }

    @Test
    fun lastUrlsFromPreferences() =
        runComposeUiTest {
            val urls = setOf("https://ooni.org", "https://ooni.io")

            val viewModel = buildViewModel(lastUrlsFromPreferences = urls)
            setContent {
                val state by viewModel.state.collectAsState()
                ChooseWebsitesScreen(state, viewModel::onEvent)
            }

            urls.forEach {
                onNodeWithText(it).assertIsDisplayed()
            }
        }

    private fun buildViewModel(
        initialUrl: String? = null,
        lastUrlsFromPreferences: Set<String>? = null,
    ) = ChooseWebsitesViewModel(
        initialUrl = initialUrl,
        onBack = {},
        goToDashboard = {},
        startBackgroundRun = { runSpec = it },
        getPreference = { flowOf(lastUrlsFromPreferences) },
        setPreference = { _, _ -> },
    )
}
