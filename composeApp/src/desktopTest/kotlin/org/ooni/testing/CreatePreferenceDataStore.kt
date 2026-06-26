package org.ooni.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File
import java.util.UUID
import org.ooni.probe.DesktopBuildConfig

internal actual fun createPreferenceDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.create {
        File(DesktopBuildConfig.BUILD_DIR, "debug-data")
            .toPath()
            .resolve("probe-${UUID.randomUUID()}.preferences_pb")
            .toFile()
    }
