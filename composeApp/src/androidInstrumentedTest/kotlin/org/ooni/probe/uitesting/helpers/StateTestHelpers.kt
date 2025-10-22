package org.ooni.probe.uitesting.helpers

import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.domain.organizationPreferenceDefaults

suspend fun skipOnboarding() {
    preferences.setValuesByKey(
        listOf(
            SettingsKey.FIRST_RUN to false,
            SettingsKey.TESTS_MOVED_NOTICE to true,
        ),
    )
}

suspend fun defaultSettings() {
    preferences.setValuesByKey(
        listOf(
            SettingsKey.UPLOAD_RESULTS to true,
            SettingsKey.AUTOMATED_TESTING_ENABLED to true,
            SettingsKey.AUTOMATED_TESTING_WIFIONLY to true,
            SettingsKey.AUTOMATED_TESTING_CHARGING to true,
        ) +
            organizationPreferenceDefaults(),
    )
}
