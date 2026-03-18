package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.ooni.passport.PassportGet
import org.ooni.probe.config.BuildTypeDefaults
import org.ooni.probe.data.models.Manifest
import org.ooni.probe.data.models.SettingsKey
import kotlin.coroutines.CoroutineContext

class RetrieveManifest(
    private val passportGet: PassportGet,
    private val json: Json,
    private val setPreference: suspend (SettingsKey, Any?) -> Unit,
    private val backgroundContext: CoroutineContext,
) {
    suspend operator fun invoke(): Manifest? =
        withContext(backgroundContext) {
            passportGet
                .get(
                    "${BuildTypeDefaults.ooniApiBaseUrl}/api/v1/manifest",
                    emptyList(),
                    emptyList(),
                ).onSuccess {
                    if (!it.isSuccessful) {
                        Logger.w("Failed to retrieve manifest (status=${it.statusCode})")
                        return@onSuccess
                    }

                    if (it.bodyText.isNullOrEmpty()) {
                        Logger.w("Failed to retrieve manifest (empty response)")
                        return@onSuccess
                    }

                    val manifest = try {
                        json.decodeFromString<Manifest>(it.bodyText)
                    } catch (e: Exception) {
                        Logger.w("Failed to parse retrieved manifest", e)
                        return@onSuccess
                    }

                    if (manifest.manifest.publicParameters.isBlank()) {
                        Logger.w("Manifest has empty publicParameters")
                        return@onSuccess
                    }

                    if (manifest.meta.version.isBlank()) {
                        Logger.w("Manifest has empty version")
                        return@onSuccess
                    }

                    setPreference(SettingsKey.MANIFEST, it.bodyText)
                    Logger.i("Manifest updated to version ${manifest.meta.version}")
                    return@withContext manifest
                }.onFailure {
                    Logger.w("Failed to retrieve manifest", it)
                }

            return@withContext null
        }
}
