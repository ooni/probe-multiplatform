package org.ooni.probe.domain

import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.SettingsKey

fun organizationPreferenceDefaults(): List<Pair<SettingsKey, Any>> =
    listOf(
        SettingsKey.MAX_RUNTIME_ENABLED to true,
        SettingsKey.MAX_RUNTIME to 90,
    ) + WebConnectivityCategory.entries
        .mapNotNull { it.settingsKey }
        .map { it to true }
