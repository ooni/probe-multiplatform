package org.ooni.probe.ui.descriptors

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.ooni.probe.data.models.DescriptorType
import org.ooni.probe.ui.descriptors.DescriptorsScreen
import org.ooni.probe.ui.descriptors.DescriptorsViewModel
import org.ooni.testing.TestLifecycleOwner
import org.ooni.testing.factories.DescriptorFactory
import kotlin.test.Test

class DescriptorsScreenTest {
    @Test
    fun showTestDescriptors() =
        runComposeUiTest {
            val descriptor = DescriptorFactory.buildDescriptorWithInstalled()
            lateinit var title: String

            setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides TestLifecycleOwner(Lifecycle.State.RESUMED)) {
                    DescriptorsScreen(
                        state =
                            DescriptorsViewModel.State(
                                sections = listOf(
                                    DescriptorsViewModel.DescriptorSection(
                                        type = DescriptorType.Installed,
                                        descriptors = listOf(descriptor),
                                    ),
                                ),
                            ),
                        onEvent = {},
                    )
                    title = descriptor.title()
                }
            }

            onNodeWithText(title).assertExists()
        }
}
