package org.ooni.probe.uitesting.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.domain.organizationPreferenceDefaults
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

suspend fun waitForBootstrap() {
    withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(3.seconds) {
            preferences.getValueByKey(SettingsKey.FIRST_RUN).filter { it != null }.first()
        }
    }
}

suspend fun skipOnboarding() {
    waitForBootstrap()
    preferences.setValuesByKey(
        listOf(
            SettingsKey.FIRST_RUN to false,
            SettingsKey.TESTS_MOVED_NOTICE to true,
        ),
    )
}

suspend fun disableRefreshArticles() {
    preferences.setValueByKey(SettingsKey.LAST_ARTICLES_REFRESH, Clock.System.now().epochSeconds)
}

suspend fun defaultSettings() {
    preferences.setValuesByKey(
        listOf(
            SettingsKey.UPLOAD_RESULTS to true,
            SettingsKey.AUTOMATED_TESTING_ENABLED to true,
            SettingsKey.AUTOMATED_TESTING_WIFIONLY to true,
            SettingsKey.AUTOMATED_TESTING_CHARGING to true,
            SettingsKey.MMDB_LAST_CHECK to Clock.System.now().toEpochMilliseconds(),
        ) +
            organizationPreferenceDefaults(),
    )
}
