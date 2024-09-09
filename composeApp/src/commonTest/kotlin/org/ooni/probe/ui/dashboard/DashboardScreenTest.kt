package org.ooni.probe.ui.dashboard

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.ooni.probe.data.models.DescriptorType
import org.ooni.testing.factories.DescriptorFactory
import kotlin.test.Test

class DashboardScreenTest {
    @Test
    fun showTestDescriptors() =
        runComposeUiTest {
            val descriptor = DescriptorFactory.buildDescriptorWithInstalled()
            lateinit var title: String

            setContent {
                DashboardScreen(
                    state =
                        DashboardViewModel.State(
                            tests = mapOf(DescriptorType.Installed to listOf(descriptor)),
                        ),
                    onEvent = {},
                )
                title = descriptor.title()
            }

            onNodeWithText(title).assertExists()
        }
}
