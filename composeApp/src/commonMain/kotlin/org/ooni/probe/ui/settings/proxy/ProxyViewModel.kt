package org.ooni.probe.ui.settings.proxy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.probe.data.models.DOMAIN_NAME
import org.ooni.probe.data.models.IPV6_ADDRESS
import org.ooni.probe.data.models.IP_ADDRESS
import org.ooni.probe.data.models.ProxyProtocol
import org.ooni.probe.data.models.ProxyType
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository

class ProxyViewModel(
    onBack: () -> Unit,
    preferenceManager: PreferenceRepository,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State(proxyProtocol = ProxyProtocol.NONE))
    val state = _state.asStateFlow()

    init {
        preferenceManager.allSettings(
            listOf(
                SettingsKey.PROXY_HOSTNAME,
                SettingsKey.PROXY_PORT,
                SettingsKey.PROXY_PROTOCOL,
            ),
        ).onEach { result ->
            _state.update {
                val proxyProtocol = ProxyProtocol.fromValue(
                    result[SettingsKey.PROXY_PROTOCOL] as? String,
                )
                it.copy(
                    proxyHost = result[SettingsKey.PROXY_HOSTNAME] as? String,
                    proxyPort = result[SettingsKey.PROXY_PORT] as? Int,
                    proxyProtocol = proxyProtocol,
                    proxyType = proxyProtocol.proxyType(),
                )
            }
        }.launchIn(viewModelScope)
        events.onEach { event ->
            when (event) {
                is Event.BackClicked -> {
                    state.value.run {
                        preferenceManager.setValueByKey(key = SettingsKey.PROXY_PROTOCOL, value = proxyProtocol.value)

                        if (isValidDomainNameOrIp(proxyHost ?: "")) {
                            preferenceManager.setValueByKey(key = SettingsKey.PROXY_HOSTNAME, value = proxyHost)
                        } else {
                            _state.update { it.copy(proxyHostError = true) }
                        }

                        if (isValidPort(proxyPort.toString())) {
                            preferenceManager.setValueByKey(key = SettingsKey.PROXY_PORT, value = proxyPort)
                        } else {
                            _state.update { it.copy(proxyPortError = true) }
                        }

                        if (proxyPortError || proxyHostError || proxyProtocolError) {
                            return@run
                        } else {
                            onBack()
                        }
                    }
                }
                is Event.ProtocolChanged -> {
                    if (event.protocol.proxyType() == ProxyType.CUSTOM) {
                        _state.update {
                            it.copy(
                                proxyHostError = it.proxyHost.isNullOrEmpty(),
                                proxyPortError = it.proxyPort.toString().isEmpty(),
                                proxyProtocolError = false,
                                proxyProtocol = event.protocol,
                            )
                        }
                    }
                }
                is Event.ProxyHostChanged -> {
                    if (isValidDomainNameOrIp(event.host)) {
                        _state.update {
                            it.copy(proxyHost = event.host, proxyHostError = false)
                        }
                    } else {
                        _state.update { it.copy(proxyHostError = true) }
                    }
                }
                is Event.ProxyPortChanged -> {
                    if (isValidPort(event.port)) {
                        _state.update {
                            it.copy(proxyPort = event.port.toInt(), proxyPortError = false)
                        }
                    } else {
                        _state.update { it.copy(proxyPortError = true) }
                    }
                }
                is Event.ProtocolTypeSelected -> {
                    _state.update { it.copy(proxyType = event.protocol) }
                    val protocolName = when (event.protocol) {
                        ProxyType.NONE -> ProxyProtocol.NONE.value
                        ProxyType.PSIPHON -> ProxyProtocol.PSIPHON.value
                        ProxyType.CUSTOM -> state.value.proxyProtocol.value
                    }
                    preferenceManager.setValueByKey(key = SettingsKey.PROXY_PROTOCOL, value = protocolName)
                    if (event.protocol != ProxyType.CUSTOM) {
                        preferenceManager.remove(key = SettingsKey.PROXY_HOSTNAME)
                        preferenceManager.remove(key = SettingsKey.PROXY_PORT)
                        _state.update { it.copy(proxyHost = null, proxyPort = null, proxyHostError = false, proxyPortError = false) }
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private fun isValidPort(port: String): Boolean {
        val portInt = port.toIntOrNull() ?: return false

        return portInt in 1..65535
    }

    private fun isValidDomainNameOrIp(host: String): Boolean {
        return host.matches(IP_ADDRESS.toRegex()) || host.matches(IPV6_ADDRESS.toRegex()) || host.matches(
            DOMAIN_NAME.toRegex(),
        )
    }

    data class State(
        val proxyHost: String? = null,
        val proxyHostError: Boolean = false,
        val proxyPort: Int? = null,
        val proxyPortError: Boolean = false,
        val proxyProtocol: ProxyProtocol,
        val proxyProtocolError: Boolean = false,
        val proxyType: ProxyType = proxyProtocol.proxyType(),
    )

    sealed interface Event {
        data object BackClicked : Event

        data class ProtocolTypeSelected(val protocol: ProxyType) : Event

        data class ProtocolChanged(val protocol: ProxyProtocol) : Event

        data class ProxyHostChanged(val host: String) : Event

        data class ProxyPortChanged(val port: String) : Event
    }
}
