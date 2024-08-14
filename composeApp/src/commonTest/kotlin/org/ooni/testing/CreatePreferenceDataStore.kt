package org.ooni.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

internal expect fun createPreferenceDataStore(): DataStore<Preferences>
