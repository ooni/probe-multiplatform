package org.ooni.probe.domain

import kotlinx.coroutines.flow.first
import org.ooni.engine.models.NetworkType
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository

class ShouldShowVpnWarning(
    private val preferenceRepository: PreferenceRepository,
    private val networkTypeFinder: () -> NetworkType,
) {
    suspend operator fun invoke(): Boolean {
        val shouldWarn = preferenceRepository.getValueByKey(SettingsKey.WARN_VPN_IN_USE).first()
        val networkType = networkTypeFinder()
        return shouldWarn == true && networkType == NetworkType.VPN
    }
}
