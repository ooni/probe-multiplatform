package org.ooni.probe.uitesting.helpers

import org.ooni.probe.config.AndroidBatteryOptimization
import org.ooni.probe.data.models.SettingsKey
import org.ooni.testing.defaultSettings as defaultSettingsCommon
import org.ooni.testing.disableRefreshArticles as disableRefreshArticlesCommon
import org.ooni.testing.skipOnboarding as skipOnboardingCommon
import org.ooni.testing.waitForBootstrap as waitForBootstrapCommon

suspend fun waitForBootstrap() = waitForBootstrapCommon(preferences)

suspend fun skipOnboarding() {
    skipOnboardingCommon(preferences)
    AndroidBatteryOptimization.isSupported = false
    preferences.setValueByKey(
        SettingsKey.MANIFEST,
        "{\"manifest\":{\"nym_scope\":\"ooni.org/{probe_cc}/{probe_asn}\",\"submission_policy\":[],\"public_parameters\":\"PARAMS\"},\"meta\":{\"version\":\"1.0.0\",\"last_modification_date\":\"2026-03-02T18:28:59.841Z\",\"manifest_url\":\"https://example.org\"}}",
    )
}

suspend fun disableRefreshArticles() = disableRefreshArticlesCommon(preferences)

suspend fun defaultSettings() = defaultSettingsCommon(preferences)
