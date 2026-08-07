package org.ooni.probe.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import org.ooni.probe.shared.toEpoch
import org.ooni.probe.shared.toLocalDateTime

class AppReviewRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private val launchTimesKey by lazy { longPreferencesKey("launch_times") }
    private val firstOpenAtKey by lazy { longPreferencesKey("first_open_at") }
    private val shownAtKey by lazy { longPreferencesKey("app_review_shown_at") }

    suspend fun incrementLaunchTimes() {
        dataStore.edit {
            it[launchTimesKey] = (it[launchTimesKey] ?: 0) + 1
        }
    }

    suspend fun getLaunchTimes() = dataStore.data.map { it[launchTimesKey] ?: 0 }.first()

    suspend fun setFirstOpenAt(value: LocalDateTime) {
        dataStore.edit {
            it[firstOpenAtKey] = value.toEpoch()
        }
    }

    suspend fun getFirstOpenAt() = dataStore.data.map { it[firstOpenAtKey]?.toLocalDateTime() }.first()

    suspend fun setShownAt(value: LocalDateTime) {
        dataStore.edit {
            it[shownAtKey] = value.toEpoch()
        }
    }

    suspend fun getShownAt() = dataStore.data.map { it[shownAtKey]?.toLocalDateTime() }.first()
}
