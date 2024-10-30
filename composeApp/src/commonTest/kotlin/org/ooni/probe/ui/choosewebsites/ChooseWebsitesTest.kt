package org.ooni.probe.ui.choosewebsites

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.RunSpecification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ChooseWebsitesTest {
    private var runSpec: RunSpecification? = null
    private val viewModel = ChooseWebsitesViewModel(
        onBack = {},
        goToDashboard = {},
        startBackgroundRun = { runSpec = it },
    )

    @Test
    fun showTestDescriptors() =
        runComposeUiTest {
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
                onNodeWithText("Add website").performClick()
            }
            websites.forEachIndexed { index, url ->
                onAllNodesWithTag("ChooseWebsite-UrlField")[index]
                    .performTextReplacement(url)
            }
            onNodeWithText("Test ${websites.size} URLs").performClick()

            val spec = runSpec
            assertNotNull(spec)
            assertEquals(false, spec.isRerun)
            assertEquals(TaskOrigin.OoniRun, spec.taskOrigin)
            assertEquals(1, spec.tests.size)
            assertEquals(TestType.WebConnectivity, spec.tests.first().netTests.first().test)
            assertEquals(websites, spec.tests.first().netTests.first().inputs)
        }
}
