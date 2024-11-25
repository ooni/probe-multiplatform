package org.ooni.probe.domain

import org.ooni.probe.data.models.PreferenceItem
import org.ooni.probe.data.models.SettingsKey

fun webConnectivityPreferences(
    enabledCategoriesCount: Int,
    maxRuntimeEnabled: Boolean,
    maxRuntime: Int?,
): List<PreferenceItem> {
    return emptyList()
}

fun preferenceDefaults(): List<Pair<SettingsKey,Any>> {
    return emptyList()
}
