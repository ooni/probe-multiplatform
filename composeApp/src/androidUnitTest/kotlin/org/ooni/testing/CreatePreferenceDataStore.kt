package org.ooni.testing

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import org.ooni.probe.di.CoreDependencies

internal actual fun createPreferenceDataStore(): DataStore<Preferences> {
    val app = ApplicationProvider.getApplicationContext<Application>()
    return CoreDependencies.getDataStore(
        producePath = { app.filesDir.resolve("test" + CoreDependencies.Companion.DATA_STORE_FILE_NAME).absolutePath },
    )
}
