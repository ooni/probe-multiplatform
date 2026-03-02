package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.ooni.engine.models.Result
import org.ooni.passport.PassportBridge
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.config.BuildTypeDefaults
import org.ooni.probe.data.models.Manifest
import org.ooni.probe.data.models.SettingsKey
import kotlin.coroutines.CoroutineContext

class RetrieveManifest(
    private val getManifest: () -> Flow<Manifest?>,
    private val passportGet: (
        String,
        List<PassportBridge.KeyValue>,
        List<PassportBridge.KeyValue>,
    ) -> Result<PassportHttpResponse, PassportException>,
    private val json: Json,
    private val setPreference: suspend (SettingsKey, Any?) -> Unit,
    private val backgroundContext: CoroutineContext,
) {
    suspend operator fun invoke() {
        withContext(backgroundContext) {
            if (getManifest().first() != null) return@withContext

            passportGet(
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
