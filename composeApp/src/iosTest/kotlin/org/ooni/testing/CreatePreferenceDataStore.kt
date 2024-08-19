package org.ooni.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.ooni.probe.di.Dependencies
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal actual fun createPreferenceDataStore(): DataStore<Preferences> {
    return Dependencies.getDataStore(
        producePath = {
            val documentDirectory: NSURL? =
                NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null,
                )
            requireNotNull(documentDirectory).path + "/${Dependencies.Companion.DATA_STORE_FILE_NAME}.test"
        },
    )
}
