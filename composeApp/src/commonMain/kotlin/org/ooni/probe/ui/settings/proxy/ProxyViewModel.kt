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
                val proxyProtocol = (result[SettingsKey.PROXY_PROTOCOL] as String?)?.let { protocol ->
                    ProxyProtocol.valueOf(protocol)
                } ?: ProxyProtocol.NONE
                it.copy(
                    proxyHost = result[SettingsKey.PROXY_HOSTNAME] as String?,
                    proxyPort = result[SettingsKey.PROXY_PORT] as Int?,
                    proxyProtocol = proxyProtocol,
                    proxyType = proxyProtocol.proxyType(),
                )
            }
        }.launchIn(viewModelScope)
        events.filterIsInstance<Event.BackClicked>().onEach { onBack() }.launchIn(viewModelScope)
        events.filterIsInstance<Event.ProtocolChanged>().onEach {
            preferenceManager.setValueByKey(
                key = SettingsKey.PROXY_PROTOCOL,
                value = it.protocol.name,
            )
        }.launchIn(viewModelScope)
        events.filterIsInstance<Event.ProxyHostChanged>().onEach {
            preferenceManager.setValueByKey(
                key = SettingsKey.PROXY_HOSTNAME,
                value = it.host,
            )
        }.launchIn(viewModelScope)
        events.filterIsInstance<Event.ProxyPortChanged>().onEach {
            preferenceManager.setValueByKey(
                key = SettingsKey.PROXY_PORT,
                value = it.port,
            )
        }.launchIn(viewModelScope)
        events.filterIsInstance<Event.ProtocolTypeSelected>().onEach { protocol ->
            _state.update { it.copy(proxyType = protocol.protocol) }
            preferenceManager.setValueByKey(
                key = SettingsKey.PROXY_PROTOCOL,
                value = when (protocol.protocol) {
                    ProxyType.NONE -> ProxyProtocol.NONE.name
                    ProxyType.PSIPHON -> ProxyProtocol.PSIPHON.name
                    ProxyType.CUSTOM -> state.value.proxyProtocol.name
                },
            )
            if (protocol.protocol != ProxyType.CUSTOM) {
                preferenceManager.remove(key = SettingsKey.PROXY_HOSTNAME)
                preferenceManager.remove(key = SettingsKey.PROXY_PORT)
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val proxyHost: String? = null,
        val proxyPort: Int? = null,
        val proxyProtocol: ProxyProtocol,
        val proxyType: ProxyType = proxyProtocol.proxyType(),
    )

    sealed interface Event {
        data object BackClicked : Event

        data class ProtocolTypeSelected(val protocol: ProxyType) : Event

        data class ProtocolChanged(val protocol: ProxyProtocol) : Event

        data class ProxyHostChanged(val host: String) : Event

        data class ProxyPortChanged(val port: Int) : Event
    }
}
