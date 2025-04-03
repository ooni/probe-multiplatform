package org.ooni.probe.domain

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import org.ooni.probe.data.models.SettingsKey
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

object PreferenceMigration : DataMigration<Preferences> {
    override suspend fun cleanUp() {
        NSBundle.mainBundle.bundleIdentifier?.let { NSUserDefaults.standardUserDefaults.removePersistentDomainForName(it) }
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        return NSUserDefaults.standardUserDefaults.dictionaryRepresentation().containsKey(
            SettingsKey.FIRST_RUN.value,
        ) && currentData.asMap().isEmpty()
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val newPreferences = currentData.toMutablePreferences()

        newPreferences[booleanPreferencesKey(SettingsKey.FIRST_RUN.value)] =
            NSUserDefaults.standardUserDefaults.boolForKey(SettingsKey.FIRST_RUN.value)

        (
            listOf(
                SettingsKey.SEND_CRASH to true,
                SettingsKey.UPLOAD_RESULTS to true,
                SettingsKey.AUTOMATED_TESTING_ENABLED to true,
                SettingsKey.AUTOMATED_TESTING_WIFIONLY to true,
                SettingsKey.AUTOMATED_TESTING_CHARGING to true,
            ) +
                organizationPreferenceDefaults()
        ).forEach { (settingsKey, value) ->
            when (value) {
                is Int -> newPreferences[intPreferencesKey(settingsKey.value)] = value

                is String -> newPreferences[stringPreferencesKey(settingsKey.value)] = value

                is Boolean -> newPreferences[booleanPreferencesKey(settingsKey.value)] = value

                is Float -> newPreferences[floatPreferencesKey(settingsKey.value)] = value

                is Long -> newPreferences[longPreferencesKey(settingsKey.value)] = value
            }
        }

        NSUserDefaults.standardUserDefaults.arrayForKey("categories_disabled")
            ?.let { disabledCategories ->
                disabledCategories.mapNotNull { it as? String }.forEach { category ->
                    newPreferences[booleanPreferencesKey(category)] = false
                }
            } ?: Logger.w("'categories_disabled' key not found in NSUserDefaults")

        return newPreferences
    }
}
