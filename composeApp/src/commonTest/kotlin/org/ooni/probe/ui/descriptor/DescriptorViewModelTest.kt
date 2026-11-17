package org.ooni.probe.ui.descriptor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorUpdateOperationState
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.SessionState
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.testing.createPreferenceDataStore
import org.ooni.testing.factories.DescriptorFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Clock

class DescriptorViewModelTest {
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
            val email = "author@example.org"
            val session = AuthSession(
                sessionToken = "token",
                emailAddress = email,
                role = "user",
                loginTime = Clock.System.now(),
            )
            val descriptorItem = DescriptorFactory
                .buildDescriptorWithInstalled()
                .copy(
                    descriptor = DescriptorFactory.buildInstalledModel(
                        id = Descriptor.Id("desc_1"),
                        author = email,
                    ),
                )

            val viewModel = DescriptorViewModel(
                descriptorId = Descriptor.Id("desc_1"),
                onBack = {},
                goToReviewDescriptorUpdates = {},
                goToChooseWebsites = {},
                goToResult = {},
                goToDescriptorWebsites = {},
                goToEditDescriptor = {},
                getTestDescriptor = { flowOf(descriptorItem) },
                getLastResultOfDescriptor = { flowOf(null) },
                preferenceRepository = PreferenceRepository(createPreferenceDataStore()),
                launchAction = { false },
                shouldShowVpnWarning = { false },
                deleteTestDescriptor = {},
                startDescriptorsUpdate = {},
                setAutoUpdate = { _, _ -> },
                observeDescriptorsUpdateState = {
                    flowOf(DescriptorsUpdateState(operationState = DescriptorUpdateOperationState.Idle))
                },
                dismissDescriptorReviewNotice = {},
                undoRejectedDescriptorUpdate = {},
                startBackgroundRun = {},
                canPullToRefresh = true,
                isDescriptorEditable = { flowOf(false) },
                observeSessionState = { flowOf(SessionState.LoggedIn(session)) },
                clearSession = { clearSessionCalled = true },
            )

            viewModel.onEvent(DescriptorViewModel.Event.LogOutClicked)

            assertTrue(clearSessionCalled)
        }

    @Test
    fun isCreatedByUserReturnsTrueOnlyWhenAuthorMatchesSessionEmail() =
        runTest(dispatcher) {
            val authorEmail = "author@example.org"
            val session = AuthSession(
                sessionToken = "token",
                emailAddress = authorEmail,
                role = "user",
                loginTime = Clock.System.now(),
            )
            val descriptorItem = DescriptorFactory
                .buildDescriptorWithInstalled()
                .copy(
                    descriptor = DescriptorFactory.buildInstalledModel(
                        id = Descriptor.Id("desc_1"),
                        author = authorEmail,
                    ),
                )

            val viewModel = DescriptorViewModel(
                descriptorId = Descriptor.Id("desc_1"),
                onBack = {},
                goToReviewDescriptorUpdates = {},
                goToChooseWebsites = {},
                goToResult = {},
                goToDescriptorWebsites = {},
                goToEditDescriptor = {},
                getTestDescriptor = { flowOf(descriptorItem) },
                getLastResultOfDescriptor = { flowOf(null) },
                preferenceRepository = PreferenceRepository(createPreferenceDataStore()),
                launchAction = { false },
                shouldShowVpnWarning = { false },
                deleteTestDescriptor = {},
                startDescriptorsUpdate = {},
                setAutoUpdate = { _, _ -> },
                observeDescriptorsUpdateState = {
                    flowOf(DescriptorsUpdateState(operationState = DescriptorUpdateOperationState.Idle))
                },
                dismissDescriptorReviewNotice = {},
                undoRejectedDescriptorUpdate = {},
                startBackgroundRun = {},
                canPullToRefresh = true,
                isDescriptorEditable = { flowOf(false) },
                observeSessionState = { flowOf(SessionState.LoggedIn(session)) },
                clearSession = {},
            )

            val state = viewModel.state.first { it.descriptor != null }
            assertTrue(state.isCreatedByUser)
        }
}
