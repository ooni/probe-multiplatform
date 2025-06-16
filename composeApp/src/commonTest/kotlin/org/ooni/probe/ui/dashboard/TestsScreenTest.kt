package org.ooni.probe.ui.dashboard

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.ooni.probe.data.models.DescriptorType
import org.ooni.probe.ui.tests.TestsScreen
import org.ooni.probe.ui.tests.TestsViewModel
import org.ooni.testing.factories.DescriptorFactory
import kotlin.test.Test

class TestsScreenTest {
    @Test
    fun showTestDescriptors() =
        runComposeUiTest {
            val descriptor = DescriptorFactory.buildDescriptorWithInstalled()
            lateinit var title: String

            setContent {
                TestsScreen(
                    state =
                        TestsViewModel.State(
                            descriptors = mapOf(DescriptorType.Installed to listOf(descriptor)),
                        ),
                    onEvent = {},
                )
                title = descriptor.title()
            }

            onNodeWithText(title).assertExists()
        }
}
