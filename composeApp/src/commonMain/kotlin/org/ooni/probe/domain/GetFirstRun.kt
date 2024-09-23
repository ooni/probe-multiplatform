package org.ooni.probe.domain

import kotlinx.coroutines.flow.map
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository

class GetFirstRun(
    private val preferenceRepository: PreferenceRepository,
) {
    operator fun invoke() =
        preferenceRepository.getValueByKey(SettingsKey.FIRST_RUN)
            .map { it != false }
}
