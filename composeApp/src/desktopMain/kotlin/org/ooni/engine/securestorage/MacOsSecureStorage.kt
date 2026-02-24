package org.ooni.engine.securestorage

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import org.ooni.engine.SecureStorage

/**
 * macOS implementation of [org.ooni.engine.SecureStorage] using the Keychain Services API.
 *
 * Stores credentials as generic passwords with service = [appId]
 * and account = key. Uses the legacy Keychain API (SecKeychainAddGenericPassword,
 * etc.) which is simpler to call via JNA than the modern SecItem* API.
 *
 * Since the legacy Keychain API lacks a simple enumerate-by-service function,
 * a key index entry is maintained to support [list] and [deleteAll].
 */
class MacOsSecureStorage(
    private val appId: String,
    baseSoftwareName: String,
) : SecureStorage {
    private val keyIndexKey = "__${baseSoftwareName}_key_index__"

    companion object {
        // Use newline as separator — null bytes are truncated by C string APIs
        private const val KEY_INDEX_SEPARATOR = "\n"

        private const val ERR_SEC_SUCCESS = 0
        private const val ERR_SEC_ITEM_NOT_FOUND = -25300
        private const val ERR_SEC_DUPLICATE_ITEM = -25299
    }

    @Suppress("FunctionName")
    private interface Security : Library {
        fun SecKeychainAddGenericPassword(
            keychain: Pointer?,
            serviceNameLength: Int,
            serviceName: String,
            accountNameLength: Int,
            accountName: String,
            passwordLength: Int,
            passwordData: ByteArray,
            itemRef: PointerByReference?,
        ): Int

        fun SecKeychainFindGenericPassword(
            keychainOrArray: Pointer?,
            serviceNameLength: Int,
            serviceName: String,
            accountNameLength: Int,
            accountName: String,
            passwordLength: IntByReference?,
            passwordData: PointerByReference?,
            itemRef: PointerByReference?,
        ): Int

        fun SecKeychainItemModifyAttributesAndData(
            itemRef: Pointer,
            attrList: Pointer?,
            length: Int,
            data: ByteArray,
        ): Int

        fun SecKeychainItemDelete(itemRef: Pointer): Int

        fun SecKeychainItemFreeContent(
            attrList: Pointer?,
            data: Pointer?,
        ): Int

        fun CFRelease(cf: Pointer)

        companion object {
            val INSTANCE: Security =
                Native.load("Security", Security::class.java)
        }
    }

    private val lib = Security.INSTANCE

    private fun rawRead(key: String): String? {
        val passwordLength = IntByReference()
        val passwordData = PointerByReference()
        val serviceBytes = appId.toByteArray(Charsets.UTF_8)
        val accountBytes = key.toByteArray(Charsets.UTF_8)

        val status =
            lib.SecKeychainFindGenericPassword(
                null,
                serviceBytes.size,
                appId,
                accountBytes.size,
                key,
                passwordLength,
                passwordData,
                null,
            )

        if (status != ERR_SEC_SUCCESS) return null

        val data = passwordData.value ?: return null
        val length = passwordLength.value

        try {
            if (length == 0) return ""
            val bytes = data.getByteArray(0, length)
            return String(bytes, Charsets.UTF_8)
        } finally {
            lib.SecKeychainItemFreeContent(null, data)
        }
    }

    private fun rawWrite(
        key: String,
        value: String,
    ): Boolean {
        val passwordBytes = value.toByteArray(Charsets.UTF_8)
        val serviceBytes = appId.toByteArray(Charsets.UTF_8)
        val accountBytes = key.toByteArray(Charsets.UTF_8)

        val itemRef = PointerByReference()
        val addStatus =
            lib.SecKeychainAddGenericPassword(
                null,
                serviceBytes.size,
                appId,
                accountBytes.size,
                key,
                passwordBytes.size,
                passwordBytes,
                itemRef,
            )

        if (addStatus == ERR_SEC_SUCCESS) {
            if (itemRef.value != null) lib.CFRelease(itemRef.value)
            return true
        }

        if (addStatus == ERR_SEC_DUPLICATE_ITEM) {
            val existingRef = PointerByReference()
            val findStatus =
                lib.SecKeychainFindGenericPassword(
                    null,
                    serviceBytes.size,
                    appId,
                    accountBytes.size,
                    key,
                    null,
                    null,
                    existingRef,
                )
            if (findStatus != ERR_SEC_SUCCESS || existingRef.value == null) return false
            try {
                return lib.SecKeychainItemModifyAttributesAndData(
                    existingRef.value,
                    null,
                    passwordBytes.size,
                    passwordBytes,
                ) == ERR_SEC_SUCCESS
            } finally {
                lib.CFRelease(existingRef.value)
            }
        }

        return false
    }

    private fun rawDelete(key: String): Boolean {
        val serviceBytes = appId.toByteArray(Charsets.UTF_8)
        val accountBytes = key.toByteArray(Charsets.UTF_8)
        val itemRef = PointerByReference()

        val findStatus =
            lib.SecKeychainFindGenericPassword(
                null,
                serviceBytes.size,
                appId,
                accountBytes.size,
                key,
                null,
                null,
                itemRef,
            )

        if (findStatus != ERR_SEC_SUCCESS || itemRef.value == null) return false

        try {
            return lib.SecKeychainItemDelete(itemRef.value) == ERR_SEC_SUCCESS
        } finally {
            lib.CFRelease(itemRef.value)
        }
    }

    private fun readIndex(): Set<String> {
        val indexValue = rawRead(keyIndexKey) ?: return emptySet()
        return indexValue.split(KEY_INDEX_SEPARATOR).filter { it.isNotEmpty() }.toSet()
    }

    private fun writeIndex(keys: Set<String>) {
        rawWrite(keyIndexKey, keys.joinToString(KEY_INDEX_SEPARATOR))
    }

    override suspend fun read(key: String): String? = rawRead(key)

    override suspend fun write(
        key: String,
        value: String,
    ): Boolean {
        val success = rawWrite(key, value)
        if (success) {
            val keys = readIndex().toMutableSet()
            keys.add(key)
            writeIndex(keys)
        }
        return success
    }

    override suspend fun exists(key: String): Boolean {
        val serviceBytes = appId.toByteArray(Charsets.UTF_8)
        val accountBytes = key.toByteArray(Charsets.UTF_8)

        return lib.SecKeychainFindGenericPassword(
            null,
            serviceBytes.size,
            appId,
            accountBytes.size,
            key,
            null,
            null,
            null,
        ) == ERR_SEC_SUCCESS
    }

    override suspend fun delete(key: String): Boolean {
        rawDelete(key)
        val keys = readIndex().toMutableSet()
        keys.remove(key)
        writeIndex(keys)
        return true
    }

    override suspend fun list(): List<String> = readIndex().toList()

    override suspend fun deleteAll(): Boolean {
        val keys = readIndex()
        for (key in keys) {
            rawDelete(key)
        }
        rawDelete(keyIndexKey)
        return true
    }
}
