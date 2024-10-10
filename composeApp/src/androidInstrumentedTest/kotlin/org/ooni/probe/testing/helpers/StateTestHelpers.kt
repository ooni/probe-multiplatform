package org.ooni.probe.testing.helpers

import org.ooni.probe.data.models.SettingsKey

suspend fun skipOnboarding() {
    preferences.setValueByKey(SettingsKey.FIRST_RUN, false)
}
