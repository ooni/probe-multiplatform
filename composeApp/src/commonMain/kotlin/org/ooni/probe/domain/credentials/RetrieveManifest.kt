package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.ooni.passport.PassportGet
import org.ooni.probe.config.BuildTypeDefaults
import org.ooni.probe.data.models.Manifest
import org.ooni.probe.data.models.SettingsKey
import kotlin.coroutines.CoroutineContext

class RetrieveManifest(
    private val getManifest: () -> Flow<Manifest?>,
    private val passportGet: PassportGet,
    private val json: Json,
    private val setPreference: suspend (SettingsKey, Any?) -> Unit,
    private val backgroundContext: CoroutineContext,
) {
    suspend operator fun invoke() {
        withContext(backgroundContext) {
            if (getManifest().first() != null) return@withContext

            passportGet
                .get(
                    "${BuildTypeDefaults.ooniApiBaseUrl}/api/v1/manifest",
                    emptyList(),
                    emptyList(),
                ).onSuccess {
                    if (!it.isSuccessful) {
                        Logger.w("Failed to retrieve manifest (status=${it.statusCode})")
                        return@withContext
                    }

                    if (it.bodyText.isNullOrEmpty()) {
                        Logger.w("Failed to retrieve manifest (empty response)")
                        return@withContext
                    }

                    try {
                        val manifest = json.decodeFromString<Manifest>(it.bodyText)

                        if (manifest.manifest.publicParameters.isBlank()) {
                            Logger.w("Manifest has empty publicParameters")
                            return@withContext
                        }

                        if (manifest.meta.version.isBlank()) {
                            Logger.w("Manifest has empty version")
                            return@withContext
                        }

                        Logger.i("Manifest updated to version ${manifest.meta.version}")
                    } catch (e: Exception) {
                        Logger.w("Failed to parse retrieved manifest", e)
                        return@withContext
                    }

                    setPreference(SettingsKey.MANIFEST, it.bodyText)
                }.onFailure {
                    Logger.w("Failed to retrieve manifest", it)
                }
        }
    }
}
