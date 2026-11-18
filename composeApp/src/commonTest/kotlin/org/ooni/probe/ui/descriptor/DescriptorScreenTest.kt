package org.ooni.probe.ui.descriptor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import ooniprobe.composeapp.generated.resources.CreateDescriptor_LoggedInAs
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.AuthSession
import org.ooni.testing.TestLifecycleOwner
import org.ooni.testing.factories.DescriptorFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class DescriptorScreenTest {
    @Test
    fun showsLoggedInHeaderOnlyWhenCreatedByUserAndLoggedIn() =
        runComposeUiTest {
            val email = "user@example.org"
            val descriptorItem = DescriptorFactory.buildDescriptorWithInstalled()
            lateinit var loggedInAsText: String

            setContent {
                loggedInAsText = stringResource(Res.string.CreateDescriptor_LoggedInAs, email)

                CompositionLocalProvider(
                    LocalLifecycleOwner provides TestLifecycleOwner(Lifecycle.State.RESUMED),
                    LocalNavigationEventDispatcherOwner provides rememberNavigationEventDispatcherOwner(parent = null),
                ) {
                    DescriptorScreen(
                        state = DescriptorViewModel.State(
                            descriptor = descriptorItem,
                            session = AuthSession(
                                sessionToken = "token",
                                emailAddress = email,
                                role = "user",
                                loginTime = Clock.System.now(),
                            ),
                        ),
                        onEvent = {},
                    )
                }
            }

            // session is present, but isCreatedByUser is false by default when author doesn't match
            onNodeWithText(loggedInAsText).assertDoesNotExist()
        }

    @Test
    fun showsLoggedInHeaderWhenCreatedByUserIsTrue() =
        runComposeUiTest {
            val email = "user@example.org"
            val descriptorItem = DescriptorFactory.buildDescriptorWithInstalled().copy(
                descriptor = DescriptorFactory.buildInstalledModel(author = email),
            )
            lateinit var loggedInAsText: String

            setContent {
                loggedInAsText = stringResource(Res.string.CreateDescriptor_LoggedInAs, email)

                CompositionLocalProvider(
                    LocalLifecycleOwner provides TestLifecycleOwner(Lifecycle.State.RESUMED),
                    LocalNavigationEventDispatcherOwner provides rememberNavigationEventDispatcherOwner(parent = null),
                ) {
                    DescriptorScreen(
                        state = DescriptorViewModel.State(
                            descriptor = descriptorItem,
                            session = AuthSession(
                                sessionToken = "token",
                                emailAddress = email,
                                role = "user",
                                loginTime = Clock.System.now(),
                            ),
                        ),
                        onEvent = {},
                    )
                }
            }

            onNodeWithText(loggedInAsText).assertExists()
        }

    @Test
    fun tappingLogOutEmitsLogOutClickedEvent() =
        runComposeUiTest {
            val email = "user@example.org"
            val descriptorItem = DescriptorFactory.buildDescriptorWithInstalled().copy(
                descriptor = DescriptorFactory.buildInstalledModel(author = email),
            )
            var emittedEvent: DescriptorViewModel.Event? = null

            setContent {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides TestLifecycleOwner(Lifecycle.State.RESUMED),
                    LocalNavigationEventDispatcherOwner provides rememberNavigationEventDispatcherOwner(parent = null),
                ) {
                    DescriptorScreen(
                        state = DescriptorViewModel.State(
                            descriptor = descriptorItem,
                            session = AuthSession(
                                sessionToken = "token",
                                emailAddress = email,
                                role = "user",
                                loginTime = Clock.System.now(),
                            ),
                        ),
                        onEvent = { emittedEvent = it },
                    )
                }
            }

            onNodeWithTag("LoggedInHeader-LogOut").performClick()
            assertEquals(DescriptorViewModel.Event.LogOutClicked, emittedEvent)
        }
}
