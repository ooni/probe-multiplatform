package org.ooni.probe.domain.credentials

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.ooni.probe.data.models.Manifest
import org.ooni.probe.data.models.SettingsKey

class GetManifest(
    val getPreference: (SettingsKey) -> Flow<Any?>,
    val json: Json,
) {
    operator fun invoke() =
        getPreference(SettingsKey.MANIFEST)
            .map { value ->
                try {
                    (value as? String)?.let {
                        json.decodeFromString<Manifest>(it)
                    }
                } catch (e: Exception) {
                    null
                }
            }
}
