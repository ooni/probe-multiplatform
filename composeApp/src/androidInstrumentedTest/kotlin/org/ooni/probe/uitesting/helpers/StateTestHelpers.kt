package org.ooni.probe.uitesting.helpers

import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.domain.organizationPreferenceDefaults

suspend fun skipOnboarding() {
    preferences.setValueByKey(SettingsKey.FIRST_RUN, false)
}

suspend fun defaultSettings() {
    preferences.setValuesByKey(
        listOf(
            SettingsKey.UPLOAD_RESULTS to true,
            SettingsKey.AUTOMATED_TESTING_ENABLED to true,
            SettingsKey.AUTOMATED_TESTING_WIFIONLY to true,
            SettingsKey.AUTOMATED_TESTING_CHARGING to true,
            SettingsKey.NOTIFICATIONS_ENABLED to true,
        ) +
            organizationPreferenceDefaults(),
    )
}
