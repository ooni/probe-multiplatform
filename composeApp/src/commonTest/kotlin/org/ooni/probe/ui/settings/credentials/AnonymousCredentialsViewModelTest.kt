package org.ooni.probe.ui.settings.credentials

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.ooni.probe.domain.credentials.AnonymousCredentialsHealth
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnonymousCredentialsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler())

    private val ready = AnonymousCredentialsHealth.Ready(
        probeId = "probe-id",
        probeAsn = "AS123",
        probeCc = "CM",
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun healthIsLoadedOnStart() =
        runTest(dispatcher) {
            val viewModel = buildViewModel(health = { ready })

            val state = viewModel.state.first()
            assertEquals(ready, state.health)
            assertFalse(state.isLoading)
            assertNull(state.resetOutcome)
        }

    @Test
    fun resetClearsThenRegistersAndRefreshesHealth() =
        runTest(dispatcher) {
            val calls = mutableListOf<String>()
            var healthAfterReset = false
            val viewModel = buildViewModel(
                health = {
                    calls += "health"
                    if (healthAfterReset) ready else AnonymousCredentialsHealth.NoCredential
                },
                clear = { calls += "clear" },
                register = {
                    calls += "register"
                    healthAfterReset = true
                    true
                },
            )

            viewModel.onEvent(AnonymousCredentialsViewModel.Event.ResetConfirmed)

            assertEquals(listOf("health", "clear", "register", "health"), calls)
            val state = viewModel.state.first()
            assertEquals(ready, state.health)
            assertEquals(AnonymousCredentialsViewModel.ResetOutcome.Success, state.resetOutcome)
            assertFalse(state.isResetting)
        }

    @Test
    fun failedRegistrationReportsFailure() =
        runTest(dispatcher) {
            val viewModel = buildViewModel(
                health = { AnonymousCredentialsHealth.NoCredential },
                register = { false },
            )

            viewModel.onEvent(AnonymousCredentialsViewModel.Event.ResetConfirmed)

            val state = viewModel.state.first()
            assertEquals(AnonymousCredentialsViewModel.ResetOutcome.Failure, state.resetOutcome)
            assertEquals(AnonymousCredentialsHealth.NoCredential, state.health)
            assertFalse(state.isResetting)
        }

    @Test
    fun backClicked() =
        runTest(dispatcher) {
            var backPressed = false
            val viewModel = buildViewModel(onBack = { backPressed = true })

            viewModel.onEvent(AnonymousCredentialsViewModel.Event.BackClicked)

            assertTrue(backPressed)
        }

    private fun buildViewModel(
        onBack: () -> Unit = {},
        health: suspend () -> AnonymousCredentialsHealth = { ready },
        clear: suspend () -> Unit = {},
        register: suspend () -> Boolean = { true },
    ) = AnonymousCredentialsViewModel(
        onBack = onBack,
        getHealth = health,
        clearCredential = clear,
        registerCredential = register,
    )
}
