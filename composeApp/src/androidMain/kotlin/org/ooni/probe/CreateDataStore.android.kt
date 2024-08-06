package org.ooni.probe

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences

fun getDataStore(context: Context): DataStore<Preferences> =
    getDataStore(
        producePath = { context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath },
        migrations = listOf(SharedPreferencesMigration(context, "notifications_enabled")),
    )
