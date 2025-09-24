package org.ooni.probe.domain.proxy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.ooni.probe.config.ProxyConfig
import org.ooni.probe.data.models.ProxyOption
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.ui.shared.SelectableItem

class ProxyManager(
    private val getPreference: (SettingsKey) -> Flow<Any?>,
    private val setPreference: suspend (SettingsKey, Any?) -> Unit,
    private val removePreference: suspend (SettingsKey) -> Unit,
    private val proxyConfig: ProxyConfig,
) {
    fun all(): Flow<List<SelectableItem<ProxyOption>>> =
        flow {
            migrateLegacyPreferencesIfNeeded()
            emit(Unit)
        }.flatMapLatest {
            combine(
                getPreference(SettingsKey.PROXIES_CUSTOM),
                getPreference(SettingsKey.PROXY_SELECTED),
                ::Pair,
            )
        }.map { (customList, selectedValue) ->
            val customProxies = customList
                .castCustomProxiesValue()
                .map { ProxyOption.Custom(it) }
            val proxies = listOfNotNull(
                ProxyOption.None,
                if (proxyConfig.isPsiphonSupported) ProxyOption.Psiphon else null,
            ) + customProxies
            val selected = proxies.firstOrNull { it.value == selectedValue } ?: ProxyOption.None

            proxies.map {
                SelectableItem(
                    item = it,
                    isSelected = it == selected,
                )
            }
        }

    fun selected(): Flow<ProxyOption> = all().map { list -> list.first { it.isSelected }.item }

    suspend fun select(option: ProxyOption) {
        setPreference(SettingsKey.PROXY_SELECTED, option.value)
    }

    suspend fun addCustom(option: ProxyOption.Custom) {
        val customProxies = getPreference(SettingsKey.PROXIES_CUSTOM)
            .first()
            .castCustomProxiesValue()
        setPreference(
            SettingsKey.PROXIES_CUSTOM,
            (customProxies + option.value).distinct().toSet(),
        )
        setPreference(SettingsKey.PROXY_SELECTED, option.value)
    }

    suspend fun deleteCustom(option: ProxyOption.Custom) {
        val customProxies = getPreference(SettingsKey.PROXIES_CUSTOM)
            .first()
            .castCustomProxiesValue()

        if (selected().first() == option) {
            setPreference(SettingsKey.PROXY_SELECTED, ProxyOption.None.value)
        }

        setPreference(
            SettingsKey.PROXIES_CUSTOM,
            (customProxies - option.value).toSet(),
        )
    }

    private suspend fun migrateLegacyPreferencesIfNeeded() {
        val oldProtocol = getPreference(SettingsKey.LEGACY_PROXY_PROTOCOL).first() as? String
        if (oldProtocol == null) return

        if (
            oldProtocol.equals("http", ignoreCase = true) ||
            oldProtocol.equals("https", ignoreCase = true) ||
            oldProtocol.equals("socks5", ignoreCase = true)
        ) {
            val option = ProxyOption.Custom.build(
                oldProtocol,
                getPreference(SettingsKey.LEGACY_PROXY_HOSTNAME).first() as? String ?: "127.0.0.1",
                (getPreference(SettingsKey.LEGACY_PROXY_PORT).first() as? Int)?.toString()
                    ?: "1080",
            )
            addCustom(option)
        } else if (oldProtocol.equals(
                "psiphon",
                ignoreCase = true,
            ) &&
            proxyConfig.isPsiphonSupported
        ) {
            setPreference(SettingsKey.PROXY_SELECTED, ProxyOption.Psiphon.value)
        } else {
            setPreference(SettingsKey.PROXY_SELECTED, ProxyOption.None.value)
        }

        removePreference(SettingsKey.LEGACY_PROXY_PROTOCOL)
        removePreference(SettingsKey.LEGACY_PROXY_HOSTNAME)
        removePreference(SettingsKey.LEGACY_PROXY_PORT)
    }
}

private fun Any?.castCustomProxiesValue() =
    @Suppress("UNCHECKED_CAST")
    (this as? Set<String>).orEmpty().filterNot { it.isBlank() }
