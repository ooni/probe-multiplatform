package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.ooni.probe.data.models.Manifest
import kotlin.coroutines.CoroutineContext

class RegisterUserWithManifest(
    private val getManifest: GetManifest,
    private val retrieveManifest: RetrieveManifest,
    private val getCredentials: suspend () -> String?,
    private val registerUser: suspend (String, String) -> String?,
    private val backgroundContext: CoroutineContext,
) {
    suspend operator fun invoke(): String? {
        return withContext(backgroundContext) {
            try {
                val existingCredentials = getCredentials()
                if (existingCredentials != null) {
                    Logger.i("User already has credentials, skipping registration")
                    return@withContext existingCredentials
                }

                Logger.i("No existing credentials found, proceeding with registration")

                val manifest = getOrRetrieveManifest().first()
                if (manifest == null) {
                    Logger.w("No manifest available for user registration")
                    return@withContext null
                }

                val publicParameters = manifest.manifest.publicParameters
                val manifestVersion = manifest.meta.version

                Logger.i("Registering user with manifest version: $manifestVersion")

                registerUser(publicParameters, manifestVersion)
            } catch (e: Exception) {
                Logger.w("Failed to register user with manifest", e)
                null
            }
        }
    }

    private suspend fun getOrRetrieveManifest(): Flow<Manifest?> {
        retrieveManifest()
        return getManifest()
    }
}
