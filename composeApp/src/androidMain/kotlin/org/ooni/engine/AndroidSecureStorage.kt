package org.ooni.engine

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure storage backed by the Android Keystore and plain SharedPreferences.
 *
 * Each value is encrypted with AES-256-GCM using a key that never leaves the
 * hardware-backed Keystore. The stored format is Base64(IV || ciphertext).
 */
class AndroidSecureStorage(
    context: Context,
    private val prefsName: String = "ooni_secure_prefs",
    private val keyAlias: String = "ooni_secure_storage_key",
) : SecureStorage {
    private val appContext = context.applicationContext
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
        withContext(Dispatchers.IO) {
            prefs.getString(key, null)?.let { runCatching { decrypt(it) }.getOrNull() }
        }

    override suspend fun write(
        key: String,
        value: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                prefs.edit(commit = true) { putString(key, encrypt(value)) }
                true
            }.getOrDefault(false)
        }

    override suspend fun exists(key: String): Boolean =
        withContext(Dispatchers.IO) {
            prefs.contains(key)
        }

    override suspend fun delete(key: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                prefs.edit(commit = true) { remove(key) }
                true
            }.getOrDefault(false)
        }

    override suspend fun list(): List<String> =
        withContext(Dispatchers.IO) {
            prefs.all.keys.toList()
        }

    override suspend fun deleteAll(): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                prefs.edit(commit = true) { clear() }
                true
            }.getOrDefault(false)
        }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_LENGTH = 128
    }
}
