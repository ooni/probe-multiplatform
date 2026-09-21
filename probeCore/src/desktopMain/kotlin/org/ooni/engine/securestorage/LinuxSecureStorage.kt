package org.ooni.engine.securestorage

import org.ooni.engine.DeleteAllResult
import org.ooni.engine.DeleteResult
import org.ooni.engine.WriteResult

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import org.ooni.engine.SecureStorage

/**
 * Linux implementation of [org.ooni.engine.SecureStorage] using libsecret (Secret Service API).
 *
 * Stores credentials in GNOME Keyring / KDE Wallet via the freedesktop.org
 * Secret Service D-Bus interface, accessed through libsecret-1.
 *
 * Since libsecret does not provide a simple "list all passwords" function,
 * a key index entry is maintained to support [list] and [deleteAll].
 */
class LinuxSecureStorage(
    private val appId: String,
    baseSoftwareName: String,
) : SecureStorage {
    private val schemaName = "$appId.credentials"
    private val keyIndexKey = "__${baseSoftwareName}_key_index__"

    companion object {
        // Use newline as separator — null bytes are truncated by C string APIs
        private const val KEY_INDEX_SEPARATOR = "\n"
    }

    @Suppress("FunctionName")
    private interface LibSecret : Library {
        fun secret_schema_new(
            name: String,
            flags: Int,
            vararg attributes: Any?,
        ): Pointer

        fun secret_password_store_sync(
            schema: Pointer,
            collection: Pointer?,
            label: String,
            password: String,
            cancellable: Pointer?,
            error: Pointer?,
            vararg attributes: Any?,
        ): Boolean

        fun secret_password_lookup_sync(
            schema: Pointer,
            cancellable: Pointer?,
            error: Pointer?,
            vararg attributes: Any?,
        ): Pointer?

        fun secret_password_clear_sync(
            schema: Pointer,
            cancellable: Pointer?,
            error: Pointer?,
            vararg attributes: Any?,
        ): Boolean

        fun secret_password_free(password: Pointer?)

        fun secret_schema_unref(schema: Pointer)

        companion object {
            val INSTANCE: LibSecret? =
                try {
                    Native.load("secret-1", LibSecret::class.java)
                } catch (_: UnsatisfiedLinkError) {
                    null
                }

            const val SECRET_SCHEMA_ATTRIBUTE_STRING = 0
            const val SECRET_SCHEMA_NONE = 0
        }
    }

    private val lib: LibSecret =
        LibSecret.INSTANCE
            ?: throw UnsupportedOperationException(
                "libsecret-1 is not available. Install libsecret on your system.",
            )

    private fun createSchema(): Pointer =
        lib.secret_schema_new(
            schemaName,
            LibSecret.SECRET_SCHEMA_NONE,
            "key",
            LibSecret.SECRET_SCHEMA_ATTRIBUTE_STRING,
            null,
        )

    override suspend fun read(key: String): String? {
        val schema = createSchema()
        try {
            val result = lib.secret_password_lookup_sync(schema, null, null, "key", key, null)
                ?: return null
            val value = result.getString(0)
            lib.secret_password_free(result)
            return value
        } finally {
            lib.secret_schema_unref(schema)
        }
    }

    override suspend fun write(
        key: String,
        value: String,
    ): WriteResult {
        val existed = read(key) != null
        val schema = createSchema()
        val success: Boolean
        try {
            success = lib.secret_password_store_sync(
                schema,
                null,
                "$appId: $key",
                value,
                null,
                null,
                "key",
                key,
                null,
            )
        } finally {
            lib.secret_schema_unref(schema)
        }
        return if (success) {
            updateIndex { add(key) }
            if (existed) WriteResult.Updated(key) else WriteResult.Created(key)
        } else {
            WriteResult.Error(key, "write failed")
        }
    }

    override suspend fun exists(key: String): Boolean = read(key) != null

    override suspend fun delete(key: String): DeleteResult {
        // libsecret's clear_sync does not distinguish "not found" from "found+deleted",
        // so we do a lookup first to determine presence.
        val exists = read(key) != null
        if (!exists) {
            updateIndex { remove(key) }
            return DeleteResult.NotFound(key)
        }

        val schema = createSchema()
        val cleared: Boolean
        try {
            cleared = lib.secret_password_clear_sync(schema, null, null, "key", key, null)
        } finally {
            lib.secret_schema_unref(schema)
        }

        return if (cleared) {
            updateIndex { remove(key) }
            DeleteResult.Deleted(key)
        } else {
            DeleteResult.Error(key, "delete failed")
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
        // Remove index entry itself
        val indexSchema = createSchema()
        try {
            lib.secret_password_clear_sync(indexSchema, null, null, "key", keyIndexKey, null)
        } finally {
            lib.secret_schema_unref(indexSchema)
        }
        return if (hadError) {
            DeleteAllResult.Error("one or more deletions failed")
        } else {
            DeleteAllResult.DeletedCount(keys.size)
        }
    }

    private suspend fun readIndex(): Set<String> {
        val indexValue = read(keyIndexKey) ?: return emptySet()
        return indexValue.split(KEY_INDEX_SEPARATOR).filter { it.isNotEmpty() }.toSet()
    }

    private suspend fun writeIndex(keys: Set<String>) {
        val schema = createSchema()
        try {
            lib.secret_password_store_sync(
                schema,
                null,
                "$appId: $keyIndexKey",
                keys.joinToString(KEY_INDEX_SEPARATOR),
                null,
                null,
                "key",
                keyIndexKey,
                null,
            )
        } finally {
            lib.secret_schema_unref(schema)
        }
    }

    /** Reads the index, applies [block] to a mutable copy, and writes it back. */
    private suspend fun updateIndex(block: MutableSet<String>.() -> Unit) {
        val keys = readIndex().toMutableSet()
        keys.block()
        writeIndex(keys)
    }
}
