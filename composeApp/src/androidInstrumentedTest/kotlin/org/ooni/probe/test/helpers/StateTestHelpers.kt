package org.ooni.probe.test.helpers

import org.ooni.probe.data.models.SettingsKey

suspend fun skipOnboarding() {
    preferences.setValueByKey(SettingsKey.FIRST_RUN, false)
}
