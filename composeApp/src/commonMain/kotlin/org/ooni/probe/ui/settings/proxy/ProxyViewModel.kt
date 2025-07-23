package org.ooni.probe.ui.settings.proxy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import org.ooni.probe.config.ProxyConfig
import org.ooni.probe.data.models.DOMAIN_NAME
import org.ooni.probe.data.models.IPV6_ADDRESS
import org.ooni.probe.data.models.IP_ADDRESS
import org.ooni.probe.data.models.ProxyProtocol
import org.ooni.probe.data.models.ProxyType
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository

class ProxyViewModel(
    onBack: () -> Unit,
    private val preferenceManager: PreferenceRepository,
    private val proxyConfig: ProxyConfig,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(
        State(
            proxyProtocol = ProxyProtocol.NONE,
            supportedProxyTypes = proxyConfig.getSupportedProxyTypes(),
        ),
    )
    val state = _state.asStateFlow()

    init {
        preferenceManager
            .allSettings(
                listOf(
                    SettingsKey.PROXY_HOSTNAME,
                    SettingsKey.PROXY_PORT,
                    SettingsKey.PROXY_PROTOCOL,
                ),
            ).take(1)
            .onEach { result ->
                _state.update {
                    val proxyProtocol = ProxyProtocol.fromValue(
                        result[SettingsKey.PROXY_PROTOCOL] as? String,
                    )
                    it.copy(
                        proxyHost = result[SettingsKey.PROXY_HOSTNAME] as? String,
                        proxyPort = (result[SettingsKey.PROXY_PORT] as? Int)?.toString(),
                        proxyProtocol = proxyProtocol,
                        proxyType = proxyProtocol.proxyType(),
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ProtocolTypeSelected>()
            .onEach { event ->
                _state.update { state ->
                    state.copy(
                        proxyType = event.protocolType,
                        proxyProtocol = when (event.protocolType) {
                            ProxyType.NONE -> ProxyProtocol.NONE
                            ProxyType.PSIPHON -> ProxyProtocol.PSIPHON
                            ProxyType.CUSTOM ->
                                if (state.proxyProtocol.proxyType() == ProxyType.CUSTOM) {
                                    state.proxyProtocol
                                } else {
                                    ProxyProtocol.NONE
                                }
                        },
                        proxyHost = if (event.protocolType == ProxyType.CUSTOM) {
                            state.proxyHost
                        } else {
                            null
                        },
                        proxyPort = if (event.protocolType == ProxyType.CUSTOM) {
                            state.proxyPort
                        } else {
                            null
                        },
                    )
                }
                validateStateAndSave()
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ProtocolChanged>()
            .onEach { event ->
                _state.update { it.copy(proxyProtocol = event.protocol) }
                validateStateAndSave()
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ProxyHostChanged>()
            .onEach { event ->
                _state.update { it.copy(proxyHost = event.host) }
                validateStateAndSave()
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ProxyPortChanged>()
            .onEach { event ->
                _state.update { it.copy(proxyPort = event.port) }
                validateStateAndSave()
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach {
                if (validateStateAndSave()) {
                    onBack()
                }
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private suspend fun validateStateAndSave(): Boolean {
        val state = _state.value

        val isValid = if (state.proxyType == ProxyType.CUSTOM) {
            val isProtocolValid = state.proxyProtocol.proxyType() == state.proxyType
            val isHostValid = state.proxyHost?.let(::isValidDomainNameOrIp) ?: false
            val isPortValid = state.proxyPort?.let(::isValidPort) ?: false
            _state.update {
                it.copy(
                    proxyProtocolError = !isProtocolValid,
                    proxyHostError = !isHostValid,
                    proxyPortError = !isPortValid,
                )
            }
            isProtocolValid && isHostValid && isPortValid
        } else {
            true
        }

        if (isValid) {
            preferenceManager.setValueByKey(
                key = SettingsKey.PROXY_PROTOCOL,
                value = state.proxyProtocol.value,
            )
            state.proxyHost?.let {
                preferenceManager.setValueByKey(
                    key = SettingsKey.PROXY_HOSTNAME,
                    value = it,
                )
            } ?: preferenceManager.remove(SettingsKey.PROXY_HOSTNAME)
            state.proxyPort?.toIntOrNull()?.let {
                preferenceManager.setValueByKey(
                    key = SettingsKey.PROXY_PORT,
                    value = it,
                )
            } ?: preferenceManager.remove(SettingsKey.PROXY_PORT)
        }

        return isValid
    }

    private fun isValidPort(port: String): Boolean {
        val portInt = port.toIntOrNull() ?: return false

        return portInt in 1..65535
    }

    private fun isValidDomainNameOrIp(host: String): Boolean =
        host.matches(IP_ADDRESS.toRegex()) ||
            host.matches(IPV6_ADDRESS.toRegex()) ||
            host.matches(DOMAIN_NAME.toRegex())

    data class State(
        val proxyHost: String? = null,
        val proxyHostError: Boolean = false,
        val proxyPort: String? = null,
        val proxyPortError: Boolean = false,
        val proxyProtocol: ProxyProtocol,
        val proxyProtocolError: Boolean = false,
        val proxyType: ProxyType = proxyProtocol.proxyType(),
        val supportedProxyTypes: List<ProxyType> = ProxyType.entries,
    )

    sealed interface Event {
        data object BackClicked : Event

        data class ProtocolTypeSelected(
            val protocolType: ProxyType,
        ) : Event

        data class ProtocolChanged(
            val protocol: ProxyProtocol,
        ) : Event

        data class ProxyHostChanged(
            val host: String,
        ) : Event

        data class ProxyPortChanged(
            val port: String,
        ) : Event
    }
}
