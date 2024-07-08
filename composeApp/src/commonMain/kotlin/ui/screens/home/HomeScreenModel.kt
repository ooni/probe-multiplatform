package ui.screens.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import core.probe.OONIProbeClient
import core.settings.SettingsStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import platform.OONIProbeEngine

class HomeScreenModel(
    private val settingsStore: SettingsStore,
    private val ooniProbeClient: OONIProbeClient
) : ScreenModel {

    private val _httpResponse = MutableStateFlow("")
    private val _publicIP = MutableStateFlow("")
    private val _goResult = MutableStateFlow("")

    val publicIP = _publicIP.asStateFlow()
    val httpResponse = _httpResponse.asStateFlow()
    val goResult = _goResult.asStateFlow()

    fun clearSettings() {
        settingsStore.clearAll()
    }
    fun doHTTPRequest() {
        screenModelScope.launch {
            try {
                val resp = ooniProbeClient.doHTTPRequest("https://google.com/humans.txt", 2)
                _httpResponse.value = "some response"
            } catch (e: Error) {
                _httpResponse.value = "error: ${e.message}"
                Napier.e("error fetching http ${e.message}")
            }
        }
    }

    fun doGoCall() {
        _goResult.value = ooniProbeClient.doDemoCheck()
    }

    fun lookupIP() {
        screenModelScope.launch {
            try {
                val ip = ooniProbeClient.getPublicIP()
                _publicIP.value = ip
            } catch (e: Error) {
                _publicIP.value = "error: ${e.message}"
                Napier.e("error looking up IP ${e.message}")
            }
        }
    }
}