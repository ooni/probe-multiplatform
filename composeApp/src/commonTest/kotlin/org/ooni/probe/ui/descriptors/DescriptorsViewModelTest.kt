package org.ooni.probe.ui.descriptors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.data.models.DescriptorUpdateOperationState
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.SessionState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class DescriptorsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun logOutClickedCallsClearSession() =
        runTest(dispatcher) {
            var clearSessionCalled = false
            val session = AuthSession(
                sessionToken = "token",
                emailAddress = "user@example.org",
                role = "user",
                loginTime = Clock.System.now(),
            )

            val viewModel = DescriptorsViewModel(
                goToDescriptor = {},
                goToReviewDescriptorUpdates = {},
                goToAddDescriptorUrl = {},
                goToCreateDescriptor = {},
                getTestDescriptors = { flowOf(emptyList()) },
                observeDescriptorUpdateState = {
                    flowOf(DescriptorsUpdateState(operationState = DescriptorUpdateOperationState.Idle))
                },
                startDescriptorsUpdates = {},
                dismissDescriptorsUpdateNotice = {},
                canPullToRefresh = true,
                getPreference = { flowOf(null) },
                setPreference = { _, _ -> },
                observeSessionState = { flowOf(SessionState.LoggedIn(session)) },
                clearSession = { clearSessionCalled = true },
            )

            assertEquals(session, viewModel.state.value.session)

            viewModel.onEvent(DescriptorsViewModel.Event.LogOutClicked)

            assertTrue(clearSessionCalled)
        }
}
