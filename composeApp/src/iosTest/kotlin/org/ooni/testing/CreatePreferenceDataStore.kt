package org.ooni.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.ooni.probe.di.CoreDependencies
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

internal actual fun createPreferenceDataStore(): DataStore<Preferences> =
    CoreDependencies.getDataStore(
        producePath = {
            val documentDirectory: NSURL? =
                NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null,
                )
            requireNotNull(documentDirectory).path + "/test.${CoreDependencies.Companion.DATA_STORE_FILE_NAME}"
        },
    )
