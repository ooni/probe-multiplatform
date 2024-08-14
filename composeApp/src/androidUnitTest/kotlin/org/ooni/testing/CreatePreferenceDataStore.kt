package org.ooni.testing

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import org.ooni.probe.DATA_STORE_FILE_NAME
import org.ooni.probe.getDataStore

internal actual fun createPreferenceDataStore(): DataStore<Preferences> {
    val app = ApplicationProvider.getApplicationContext<Application>()
    return getDataStore(
        producePath = { app.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath },
    )
}
