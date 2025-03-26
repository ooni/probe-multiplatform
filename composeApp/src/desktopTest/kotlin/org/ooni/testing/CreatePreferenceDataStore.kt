package org.ooni.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.ooni.probe.dependencies
import java.util.UUID

internal actual fun createPreferenceDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.create {
        dependencies.cacheDir.toPath()
            .resolve("probe-${UUID.randomUUID()}.preferences_pb")
            .toFile()
    }
