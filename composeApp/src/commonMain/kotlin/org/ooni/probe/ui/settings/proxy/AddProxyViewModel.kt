package org.ooni.probe.ui.settings.proxy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.probe.data.models.CustomProxyProtocol
import org.ooni.probe.data.models.ProxyOption
import org.ooni.probe.data.models.validateHost
import org.ooni.probe.data.models.validatePassword
import org.ooni.probe.data.models.validatePort
import org.ooni.probe.data.models.validateUsername

class AddProxyViewModel(
    onBack: () -> Unit,
    private val addCustomProxy: suspend (ProxyOption.Custom) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        events
            .filterIsInstance<Event.ProtocolChanged>()
            .onEach { _state.update { state -> state.copy(protocol = it.protocol) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.HostChanged>()
            .onEach {
                _state.update { state ->
                    state.copy(
                        host = it.host,
                        showHostAsInvalid = false,
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.PortChanged>()
            .onEach {
                _state.update { state ->
                    state.copy(
                        port = it.port,
                        showPortAsInvalid = false,
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UsernameChanged>()
            .onEach {
                _state.update { state ->
                    state.copy(
                        username = it.username,
                        showUsernameAsInvalid = false,
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.PasswordChanged>()
            .onEach {
                _state.update { state ->
                    state.copy(
                        password = it.password,
                        showPasswordAsInvalid = false,
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SaveClicked>()
            .onEach {
                val state = _state.value
                val isHostValid = validateHost(state.host)
                val isPortValid = validatePort(state.port)
                val isUsernameValid = validateUsername(state.username, state.password)
                val isPasswordValid = validatePassword(state.password)

                if (isHostValid && isPortValid && isUsernameValid && isPasswordValid) {
                    addCustomProxy(
                        ProxyOption.Custom.build(
                            protocol = state.protocol.value,
                            hostname = state.host,
                            port = state.port,
                            username = state.username.takeIf { it.isNotBlank() },
                            password = state.password.takeIf { it.isNotBlank() },
                        ),
                    )
                    onBack()
                } else {
                    _state.value = state.copy(
                        showHostAsInvalid = !isHostValid,
                        showPortAsInvalid = !isPortValid,
                        showUsernameAsInvalid = !isUsernameValid,
                        showPasswordAsInvalid = !isPasswordValid,
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.TogglePasswordVisibility>()
            .onEach {
                _state.update { state ->
                    state.copy(isPasswordVisible = !state.isPasswordVisible)
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val protocol: CustomProxyProtocol = CustomProxyProtocol.HTTPS,
        val host: String = "",
        val showHostAsInvalid: Boolean = false,
        val port: String = "1080",
        val showPortAsInvalid: Boolean = false,
        val username: String = "",
        val showUsernameAsInvalid: Boolean = false,
        val password: String = "",
        val showPasswordAsInvalid: Boolean = false,
        val isPasswordVisible: Boolean = false,
    )

    sealed interface Event {
        data object BackClicked : Event

        data class ProtocolChanged(
            val protocol: CustomProxyProtocol,
        ) : Event

        data class HostChanged(
            val host: String,
        ) : Event

        data class PortChanged(
            val port: String,
        ) : Event

        data class UsernameChanged(
            val username: String,
        ) : Event

        data class PasswordChanged(
            val password: String,
        ) : Event

        data object TogglePasswordVisibility : Event

        data object SaveClicked : Event
    }
}
