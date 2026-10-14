package org.ooni.probe.ui.descriptors

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_MyLinks_Title
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_Ooni_Title
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.DescriptorType
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
                CompositionLocalProvider(
                    LocalLifecycleOwner provides TestLifecycleOwner(Lifecycle.State.RESUMED),
                    LocalNavigationEventDispatcherOwner provides rememberNavigationEventDispatcherOwner(parent = null),
                ) {
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

    @Test
    fun hidesSectionTitleWhenOnlyOneSectionExists() =
        runComposeUiTest {
            val descriptor = DescriptorFactory.buildDescriptorWithInstalled()
            lateinit var defaultTitle: String

            setContent {
                defaultTitle = stringResource(Res.string.Dashboard_RunV2_Ooni_Title).uppercase()

                CompositionLocalProvider(
                    LocalLifecycleOwner provides TestLifecycleOwner(Lifecycle.State.RESUMED),
                    LocalNavigationEventDispatcherOwner provides rememberNavigationEventDispatcherOwner(parent = null),
                ) {
                    DescriptorsScreen(
                        state =
                            DescriptorsViewModel.State(
                                sections = listOf(
                                    DescriptorsViewModel.DescriptorSection(
                                        type = DescriptorType.Default,
                                        descriptors = listOf(descriptor),
                                    ),
                                ),
                            ),
                        onEvent = {},
                    )
                }
            }

            onNodeWithText(defaultTitle).assertDoesNotExist()
        }

    @Test
    fun showsSectionTitlesWhenTwoOrMoreSectionsExist() =
        runComposeUiTest {
            val descriptor1 = DescriptorFactory.buildDescriptorWithInstalled()
            val descriptor2 = DescriptorFactory.buildDescriptorWithInstalled()
            lateinit var createdByUserTitle: String
            lateinit var defaultTitle: String

            setContent {
                createdByUserTitle = stringResource(Res.string.Dashboard_RunV2_MyLinks_Title).uppercase()
                defaultTitle = stringResource(Res.string.Dashboard_RunV2_Ooni_Title).uppercase()

                CompositionLocalProvider(
                    LocalLifecycleOwner provides TestLifecycleOwner(Lifecycle.State.RESUMED),
                    LocalNavigationEventDispatcherOwner provides rememberNavigationEventDispatcherOwner(parent = null),
                ) {
                    DescriptorsScreen(
                        state =
                            DescriptorsViewModel.State(
                                sections = listOf(
                                    DescriptorsViewModel.DescriptorSection(
                                        type = DescriptorType.CreatedByUser,
                                        descriptors = listOf(descriptor1),
                                    ),
                                    DescriptorsViewModel.DescriptorSection(
                                        type = DescriptorType.Default,
                                        descriptors = listOf(descriptor2),
                                    ),
                                ),
                            ),
                        onEvent = {},
                    )
                }
            }

            onNodeWithText(createdByUserTitle).assertExists()
            onNodeWithText(defaultTitle).assertExists()
        }
}
