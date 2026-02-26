package org.ooni.engine

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.CoroutineContext

/**
 * Secure storage backed by the Android Keystore and plain SharedPreferences.
 *
 * Each value is encrypted with AES-256-GCM using a key that never leaves the
 * hardware-backed Keystore. The stored format is Base64(IV || ciphertext).
 */
class AndroidSecureStorage(
    context: Context,
    baseSoftwareName: String,
    private val ioDispatcher: CoroutineContext = Dispatchers.IO,
) : SecureStorage {
    private val appContext = context.applicationContext
    private val prefsName = "${baseSoftwareName}_secure_prefs"
    private val keyAlias = "${baseSoftwareName}_secure_storage_key"
    private val prefs by lazy {
        appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        keyStore.getKey(keyAlias, null)?.let { return it as SecretKey }

        val keyGen =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGen.init(
            KeyGenParameterSpec
                .Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return keyGen.generateKey()
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv // GCM generates a fresh 12-byte IV per operation
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Store IV prepended to ciphertext so we can recover it on decrypt
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    private fun decrypt(encoded: String): String {
        val combined = Base64.decode(encoded, Base64.DEFAULT)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    override suspend fun read(key: String): String? =
        withContext(ioDispatcher) {
            try {
                prefs.getString(key, null)?.let { decrypt(it) }
            } catch (e: Exception) {
                Logger.w(e) { "SecureStorage: failed to read key '$key'" }
                null
            }
        }

    override suspend fun write(
        key: String,
        value: String,
    ): WriteResult =
        withContext(ioDispatcher) {
            try {
                val existed = prefs.contains(key)
                prefs.edit(commit = true) { putString(key, encrypt(value)) }
                if (existed) WriteResult.Updated(key) else WriteResult.Created(key)
            } catch (e: Exception) {
                Logger.w(e) { "SecureStorage: failed to write key '$key'" }
                WriteResult.Error(key, e.message, e)
            }
        }

    override suspend fun exists(key: String): Boolean =
        withContext(ioDispatcher) {
            prefs.contains(key)
        }

    override suspend fun delete(key: String): DeleteResult =
        withContext(ioDispatcher) {
            try {
                val existed = prefs.contains(key)
                prefs.edit(commit = true) { remove(key) }
                if (existed) DeleteResult.Deleted(key) else DeleteResult.NotFound(key)
            } catch (e: Exception) {
                Logger.w(e) { "SecureStorage: failed to delete key '$key'" }
                DeleteResult.Error(key, e.message, e)
            }
        }

    override suspend fun list(): List<String> =
        withContext(ioDispatcher) {
            prefs.all.keys.toList()
        }

    override suspend fun deleteAll(): DeleteAllResult =
        withContext(ioDispatcher) {
            try {
                val count = prefs.all.keys.size
                prefs.edit(commit = true) { clear() }
                DeleteAllResult.DeletedCount(count)
            } catch (e: Exception) {
                Logger.w(e) { "SecureStorage: failed to deleteAll" }
                DeleteAllResult.Error(e.message, e)
            }
        }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_LENGTH = 128
    }
}
