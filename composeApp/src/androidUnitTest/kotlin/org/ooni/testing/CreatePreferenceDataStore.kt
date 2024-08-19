package org.ooni.testing

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import org.ooni.probe.di.Dependencies

internal actual fun createPreferenceDataStore(): DataStore<Preferences> {
    val app = ApplicationProvider.getApplicationContext<Application>()
    return Dependencies.getDataStore(
        producePath = { app.filesDir.resolve("test" + Dependencies.Companion.DATA_STORE_FILE_NAME).absolutePath },
    )
}
