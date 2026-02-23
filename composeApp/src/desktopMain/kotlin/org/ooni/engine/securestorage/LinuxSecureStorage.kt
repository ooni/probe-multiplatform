package org.ooni.engine.securestorage

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
class LinuxSecureStorage : SecureStorage {
    companion object {
        private const val SERVICE_NAME = "org.ooni.probe"
        private const val SCHEMA_NAME = "org.ooni.probe.credentials"
        private const val KEY_INDEX_KEY = "__ooni_key_index__"

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
            SCHEMA_NAME,
            LibSecret.SECRET_SCHEMA_NONE,
            "key",
            LibSecret.SECRET_SCHEMA_ATTRIBUTE_STRING,
            null,
        )

    private fun rawRead(key: String): String? {
        val schema = createSchema()
        try {
            val result =
                lib.secret_password_lookup_sync(
                    schema,
                    null,
                    null,
                    "key",
                    key,
                    null,
                )
            if (result == null) return null
            val value = result.getString(0)
            lib.secret_password_free(result)
            return value
        } finally {
            lib.secret_schema_unref(schema)
        }
    }

    private fun rawWrite(
        key: String,
        value: String,
    ): Boolean {
        val schema = createSchema()
        try {
            return lib.secret_password_store_sync(
                schema,
                null,
                "$SERVICE_NAME: $key",
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
    }

    private fun rawDelete(key: String): Boolean {
        val schema = createSchema()
        try {
            return lib.secret_password_clear_sync(
                schema,
                null,
                null,
                "key",
                key,
                null,
            )
        } finally {
            lib.secret_schema_unref(schema)
        }
    }

    private fun readIndex(): Set<String> {
        val indexValue = rawRead(KEY_INDEX_KEY) ?: return emptySet()
        return indexValue.split(KEY_INDEX_SEPARATOR).filter { it.isNotEmpty() }.toSet()
    }

    private fun writeIndex(keys: Set<String>) {
        rawWrite(KEY_INDEX_KEY, keys.joinToString(KEY_INDEX_SEPARATOR))
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

    override suspend fun exists(key: String): Boolean = rawRead(key) != null

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
        rawDelete(KEY_INDEX_KEY)
        return true
    }
}
