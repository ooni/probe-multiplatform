package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.ooni.probe.data.models.Credential
import org.ooni.probe.data.models.Manifest
import kotlin.coroutines.CoroutineContext

class PrepareAnonymousCredentials(
    private val getManifest: () -> Flow<Manifest?>,
    private val retrieveManifest: suspend () -> Manifest?,
    private val getCredential: suspend () -> Credential?,
    private val registerUser: suspend (String, String) -> Credential?,
    private val backgroundContext: CoroutineContext,
) {
    /**
     * Serialises registration. Reconnect triggers and the background run task can both ask for
     * credentials at once; without this, two callers would each see "no credential" and register
     * twice. Waiting rather than dropping the second caller is deliberate - once the first
     * finishes, the second finds the stored credential and returns it instead of a spurious null.
     */
    private val mutex = Mutex()

    suspend operator fun invoke(): Credential? =
        withContext(backgroundContext) {
            mutex.withLock { register() }
        }

    private suspend fun register(): Credential? {
        return try {
            val existingCredential = getCredential()
            if (existingCredential != null) {
                Logger.i("User already has credential, skipping registration")
                return existingCredential
            }

            Logger.i("No existing credential found, proceeding with registration")

            val manifest = getOrRetrieveManifest()
            if (manifest == null) {
                Logger.w("No manifest available for user registration")
                return null
            }

            val publicParameters = manifest.manifest.publicParameters
            val manifestVersion = manifest.meta.version

            Logger.i("Registering user with manifest version: $manifestVersion")

            val credential = registerUser(publicParameters, manifestVersion)
            if (credential == null) {
                Logger.w("User registration failed: registerUser returned null credential")
            } else {
                Logger.i("User registered successfully: credential obtained")
            }
            credential
        } catch (e: Exception) {
            Logger.e("Failed to register user with manifest", e)
            null
        }
    }

    private suspend fun getOrRetrieveManifest(): Manifest? {
        val cached = getManifest().first()
        if (cached != null) {
            Logger.d("Using cached manifest")
            return cached
        }
        Logger.i("No cached manifest found, retrieving manifest from API")
        val retrieved = retrieveManifest()
        if (retrieved == null) {
            Logger.w("Failed to retrieve manifest from API")
        } else {
            Logger.i("Successfully retrieved manifest from API")
        }
        return retrieved
    }
}
