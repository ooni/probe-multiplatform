package org.ooni.probe.ui.settings.proxy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.ooni.probe.data.models.CustomProxyProtocol
import org.ooni.probe.data.models.ProxyOption
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddProxyViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        onBack: () -> Unit = {},
        addCustomProxy: suspend (ProxyOption.Custom) -> Unit = {},
    ) = AddProxyViewModel(
        onBack = onBack,
        addCustomProxy = addCustomProxy,
    )

    @Test
    fun saveFailsWhenPasswordIsPresentWithoutUsername() =
        runTest(dispatcher) {
            var addedProxy: ProxyOption.Custom? = null
            var backCalled = false
            val viewModel = buildViewModel(
                onBack = { backCalled = true },
                addCustomProxy = { addedProxy = it },
            )

            viewModel.onEvent(AddProxyViewModel.Event.HostChanged("example.org"))
            viewModel.onEvent(AddProxyViewModel.Event.PortChanged("1080"))
            viewModel.onEvent(AddProxyViewModel.Event.UsernameChanged(""))
            viewModel.onEvent(AddProxyViewModel.Event.PasswordChanged("secret"))
            viewModel.onEvent(AddProxyViewModel.Event.SaveClicked)

            val state = viewModel.state.first()
            assertTrue(state.showUsernameAsInvalid, "Username should be flagged invalid when password is provided")
            assertFalse(state.showPasswordAsInvalid)
            assertNull(addedProxy, "Proxy should not be added when username is missing")
            assertFalse(backCalled)
        }

    @Test
    fun saveSucceedsWithUsernameAndPassword() =
        runTest(dispatcher) {
            var addedProxy: ProxyOption.Custom? = null
            var backCalled = false
            val viewModel = buildViewModel(
                onBack = { backCalled = true },
                addCustomProxy = { addedProxy = it },
            )

            viewModel.onEvent(AddProxyViewModel.Event.ProtocolChanged(CustomProxyProtocol.SOCKS5))
            viewModel.onEvent(AddProxyViewModel.Event.HostChanged("example.org"))
            viewModel.onEvent(AddProxyViewModel.Event.PortChanged("1080"))
            viewModel.onEvent(AddProxyViewModel.Event.UsernameChanged("user"))
            viewModel.onEvent(AddProxyViewModel.Event.PasswordChanged("secret"))
            viewModel.onEvent(AddProxyViewModel.Event.SaveClicked)

            val state = viewModel.state.first()
            assertFalse(state.showUsernameAsInvalid)
            assertFalse(state.showPasswordAsInvalid)
            assertEquals("socks5://user:secret@example.org:1080/", addedProxy?.value)
            assertTrue(backCalled)
        }

    @Test
    fun saveSucceedsWithoutCredentials() =
        runTest(dispatcher) {
            var addedProxy: ProxyOption.Custom? = null
            var backCalled = false
            val viewModel = buildViewModel(
                onBack = { backCalled = true },
                addCustomProxy = { addedProxy = it },
            )

            viewModel.onEvent(AddProxyViewModel.Event.ProtocolChanged(CustomProxyProtocol.HTTP))
            viewModel.onEvent(AddProxyViewModel.Event.HostChanged("example.org"))
            viewModel.onEvent(AddProxyViewModel.Event.PortChanged("8080"))
            viewModel.onEvent(AddProxyViewModel.Event.SaveClicked)

            val state = viewModel.state.first()
            assertFalse(state.showHostAsInvalid)
            assertFalse(state.showPortAsInvalid)
            assertFalse(state.showUsernameAsInvalid)
            assertFalse(state.showPasswordAsInvalid)
            assertEquals("http://example.org:8080/", addedProxy?.value)
            assertTrue(backCalled)
        }

    @Test
    fun saveSucceedsWithUsernameOnly() =
        runTest(dispatcher) {
            var addedProxy: ProxyOption.Custom? = null
            var backCalled = false
            val viewModel = buildViewModel(
                onBack = { backCalled = true },
                addCustomProxy = { addedProxy = it },
            )

            viewModel.onEvent(AddProxyViewModel.Event.ProtocolChanged(CustomProxyProtocol.SOCKS5))
            viewModel.onEvent(AddProxyViewModel.Event.HostChanged("example.org"))
            viewModel.onEvent(AddProxyViewModel.Event.PortChanged("1080"))
            viewModel.onEvent(AddProxyViewModel.Event.UsernameChanged("user"))
            viewModel.onEvent(AddProxyViewModel.Event.PasswordChanged(""))
            viewModel.onEvent(AddProxyViewModel.Event.SaveClicked)

            val state = viewModel.state.first()
            assertFalse(state.showUsernameAsInvalid)
            assertFalse(state.showPasswordAsInvalid)
            assertEquals("socks5://user@example.org:1080/", addedProxy?.value)
            assertTrue(backCalled)
        }

    @Test
    fun saveFailsWhenUsernameOrPasswordHasInvalidCharacters() =
        runTest(dispatcher) {
            var addedProxy: ProxyOption.Custom? = null
            val viewModel = buildViewModel(
                addCustomProxy = { addedProxy = it },
            )

            viewModel.onEvent(AddProxyViewModel.Event.HostChanged("example.org"))
            viewModel.onEvent(AddProxyViewModel.Event.PortChanged("1080"))
            viewModel.onEvent(AddProxyViewModel.Event.UsernameChanged("user with space"))
            viewModel.onEvent(AddProxyViewModel.Event.PasswordChanged("pass with space"))
            viewModel.onEvent(AddProxyViewModel.Event.SaveClicked)

            val state = viewModel.state.first()
            assertTrue(state.showUsernameAsInvalid)
            assertTrue(state.showPasswordAsInvalid)
            assertNull(addedProxy)
        }

    @Test
    fun togglePasswordVisibilitySwitchesState() =
        runTest(dispatcher) {
            val viewModel = buildViewModel()

            // Initial state: password hidden
            assertFalse(viewModel.state.first().isPasswordVisible)

            // Toggle show
            viewModel.onEvent(AddProxyViewModel.Event.TogglePasswordVisibility)
            assertTrue(viewModel.state.first().isPasswordVisible)

            // Toggle hide
            viewModel.onEvent(AddProxyViewModel.Event.TogglePasswordVisibility)
            assertFalse(viewModel.state.first().isPasswordVisible)
        }

    @Test
    fun editingUsernameOrPasswordClearsInvalidErrorFlags() =
        runTest(dispatcher) {
            val viewModel = buildViewModel()

            // Trigger error state with invalid credentials
            viewModel.onEvent(AddProxyViewModel.Event.HostChanged("example.org"))
            viewModel.onEvent(AddProxyViewModel.Event.UsernameChanged("user with space"))
            viewModel.onEvent(AddProxyViewModel.Event.PasswordChanged("pass with space"))
            viewModel.onEvent(AddProxyViewModel.Event.SaveClicked)

            assertTrue(viewModel.state.first().showUsernameAsInvalid)
            assertTrue(viewModel.state.first().showPasswordAsInvalid)

            // Modifying username resets invalid state
            viewModel.onEvent(AddProxyViewModel.Event.UsernameChanged("validUser"))
            assertFalse(viewModel.state.first().showUsernameAsInvalid)
            assertTrue(viewModel.state.first().showPasswordAsInvalid)

            // Modifying password resets invalid state
            viewModel.onEvent(AddProxyViewModel.Event.PasswordChanged("validPassword"))
            assertFalse(viewModel.state.first().showPasswordAsInvalid)
        }

    @Test
    fun backClickedCallsOnBack() =
        runTest(dispatcher) {
            var backCalled = false
            val viewModel = buildViewModel(onBack = { backCalled = true })

            viewModel.onEvent(AddProxyViewModel.Event.BackClicked)

            assertTrue(backCalled)
        }
}
