package org.ooni.engine.securestorage

import org.ooni.engine.DeleteAllResult
import org.ooni.engine.DeleteResult
import org.ooni.engine.WriteResult

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

            const val ERR_SEC_SUCCESS = 0
            const val ERR_SEC_ITEM_NOT_FOUND = -25300
            const val ERR_SEC_DUPLICATE_ITEM = -25299
        }
    }

    private val lib = Security.INSTANCE

    override suspend fun read(key: String): String? {
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

        if (status != Security.ERR_SEC_SUCCESS) return null

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

    override suspend fun write(
        key: String,
        value: String,
    ): WriteResult {
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

        if (addStatus == Security.ERR_SEC_SUCCESS) {
            if (itemRef.value != null) lib.CFRelease(itemRef.value)
            updateIndex { add(key) }
            return WriteResult.Created(key)
        }

        if (addStatus == Security.ERR_SEC_DUPLICATE_ITEM) {
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
            if (findStatus != Security.ERR_SEC_SUCCESS || existingRef.value == null) {
                return WriteResult.Error(key, "osstatus=$findStatus")
            }
            try {
                val updateStatus = lib.SecKeychainItemModifyAttributesAndData(
                    existingRef.value,
                    null,
                    passwordBytes.size,
                    passwordBytes,
                )
                return if (updateStatus == Security.ERR_SEC_SUCCESS) {
                    updateIndex { add(key) }
                    WriteResult.Updated(key)
                } else {
                    WriteResult.Error(key, "osstatus=$updateStatus")
                }
            } finally {
                lib.CFRelease(existingRef.value)
            }
        }

        return WriteResult.Error(key, "osstatus=$addStatus")
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
        ) == Security.ERR_SEC_SUCCESS
    }

    override suspend fun delete(key: String): DeleteResult {
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

        if (findStatus == Security.ERR_SEC_ITEM_NOT_FOUND || itemRef.value == null) {
            updateIndex { remove(key) }
            return DeleteResult.NotFound(key)
        }

        if (findStatus != Security.ERR_SEC_SUCCESS) {
            return DeleteResult.Error(key, "osstatus=$findStatus")
        }

        try {
            val deleteStatus = lib.SecKeychainItemDelete(itemRef.value)
            return if (deleteStatus == Security.ERR_SEC_SUCCESS) {
                updateIndex { remove(key) }
                DeleteResult.Deleted(key)
            } else {
                DeleteResult.Error(key, "osstatus=$deleteStatus")
            }
        } finally {
            lib.CFRelease(itemRef.value)
        }
    }

    override suspend fun list(): List<String> = readIndex().toList()

    override suspend fun deleteAll(): DeleteAllResult {
        val keys = readIndex()
        var hadError = false
        for (key in keys) {
            val dr = delete(key)
            if (dr is DeleteResult.Error) hadError = true
        }
        // Ensure index is cleared even if some deletions failed
        writeIndex(emptySet())
        return if (hadError) DeleteAllResult.Error("one or more deletions failed") else DeleteAllResult.DeletedCount(keys.size)
    }

    private suspend fun readIndex(): Set<String> {
        val indexValue = read(keyIndexKey) ?: return emptySet()
        return indexValue.split(KEY_INDEX_SEPARATOR).filter { it.isNotEmpty() }.toSet()
    }

    private suspend fun writeIndex(keys: Set<String>) {
        val value = keys.joinToString(KEY_INDEX_SEPARATOR)
        val passwordBytes = value.toByteArray(Charsets.UTF_8)
        val serviceBytes = appId.toByteArray(Charsets.UTF_8)
        val accountBytes = keyIndexKey.toByteArray(Charsets.UTF_8)

        val itemRef = PointerByReference()
        val addStatus =
            lib.SecKeychainAddGenericPassword(
                null,
                serviceBytes.size,
                appId,
                accountBytes.size,
                keyIndexKey,
                passwordBytes.size,
                passwordBytes,
                itemRef,
            )

        if (addStatus == Security.ERR_SEC_SUCCESS) {
            if (itemRef.value != null) lib.CFRelease(itemRef.value)
            return
        }

        if (addStatus == Security.ERR_SEC_DUPLICATE_ITEM) {
            val existingRef = PointerByReference()
            val findStatus =
                lib.SecKeychainFindGenericPassword(
                    null,
                    serviceBytes.size,
                    appId,
                    accountBytes.size,
                    keyIndexKey,
                    null,
                    null,
                    existingRef,
                )
            if (findStatus == Security.ERR_SEC_SUCCESS && existingRef.value != null) {
                try {
                    lib.SecKeychainItemModifyAttributesAndData(
                        existingRef.value,
                        null,
                        passwordBytes.size,
                        passwordBytes,
                    )
                } finally {
                    lib.CFRelease(existingRef.value)
                }
            }
        }
    }

    /** Reads the index, applies [block] to a mutable copy, and writes it back. */
    private suspend fun updateIndex(block: MutableSet<String>.() -> Unit) {
        val keys = readIndex().toMutableSet()
        keys.block()
        writeIndex(keys)
    }
}
