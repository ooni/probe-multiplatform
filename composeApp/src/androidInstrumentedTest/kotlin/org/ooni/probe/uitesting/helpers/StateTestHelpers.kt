package org.ooni.probe.uitesting.helpers

import org.ooni.probe.config.AndroidBatteryOptimization
import org.ooni.testing.defaultSettings as defaultSettingsCommon
import org.ooni.testing.disableRefreshArticles as disableRefreshArticlesCommon
import org.ooni.testing.skipOnboarding as skipOnboardingCommon
import org.ooni.testing.waitForBootstrap as waitForBootstrapCommon

suspend fun waitForBootstrap() = waitForBootstrapCommon(preferences)

suspend fun skipOnboarding() {
    skipOnboardingCommon(preferences)
    AndroidBatteryOptimization.isSupported = false
}

suspend fun disableRefreshArticles() = disableRefreshArticlesCommon(preferences)

suspend fun defaultSettings() = defaultSettingsCommon(preferences)
