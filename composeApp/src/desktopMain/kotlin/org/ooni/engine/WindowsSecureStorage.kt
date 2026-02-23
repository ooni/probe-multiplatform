package org.ooni.engine

import com.sun.jna.LastErrorException
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

/**
 * Windows implementation of [SecureStorage] using the Credential Manager API.
 *
 * Stores credentials via advapi32.dll (CredWriteW, CredReadW, CredDeleteW,
 * CredEnumerateW, CredFree). All target names are prefixed with [TARGET_PREFIX]
 * to namespace entries.
 */
class WindowsSecureStorage : SecureStorage {
    companion object {
        private const val TARGET_PREFIX = "ooni-probe/"
        private const val CRED_TYPE_GENERIC = 1
        private const val CRED_PERSIST_LOCAL_MACHINE = 2
    }

    @Structure.FieldOrder(
        "Flags",
        "Type",
        "TargetName",
        "Comment",
        "LastWritten",
        "CredentialBlobSize",
        "CredentialBlob",
        "Persist",
        "AttributeCount",
        "Attributes",
        "TargetAlias",
        "UserName",
    )
    class CREDENTIAL : Structure() {
        @JvmField var Flags: Int = 0
        @JvmField var Type: Int = 0
        @JvmField var TargetName: String? = null
        @JvmField var Comment: String? = null
        @JvmField var LastWritten: WinBase.FILETIME = WinBase.FILETIME()
        @JvmField var CredentialBlobSize: Int = 0
        @JvmField var CredentialBlob: Pointer? = null
        @JvmField var Persist: Int = 0
        @JvmField var AttributeCount: Int = 0
        @JvmField var Attributes: Pointer? = null
        @JvmField var TargetAlias: String? = null
        @JvmField var UserName: String? = null
    }

    @Suppress("FunctionName")
    private interface Advapi32 : StdCallLibrary {
        fun CredWriteW(
            credential: CREDENTIAL,
            flags: Int,
        ): Boolean

        fun CredReadW(
            targetName: String,
            type: Int,
            flags: Int,
            credential: Array<Pointer?>,
        ): Boolean

        fun CredDeleteW(
            targetName: String,
            type: Int,
            flags: Int,
        ): Boolean

        fun CredEnumerateW(
            filter: String?,
            flags: Int,
            count: IntArray,
            credentials: Array<Pointer?>,
        ): Boolean

        fun CredFree(buffer: Pointer?)

        companion object {
            val INSTANCE: Advapi32 =
                Native.load("advapi32", Advapi32::class.java, W32APIOptions.UNICODE_OPTIONS)
        }
    }

    private val lib = Advapi32.INSTANCE

    private fun targetName(key: String): String = "$TARGET_PREFIX$key"

    override suspend fun read(key: String): String? {
        val pCredential = arrayOfNulls<Pointer>(1)
        val success = lib.CredReadW(targetName(key), CRED_TYPE_GENERIC, 0, pCredential)
        if (!success || pCredential[0] == null) return null

        try {
            val cred = Structure.newInstance(CREDENTIAL::class.java, pCredential[0]!!)
            cred.read()
            if (cred.CredentialBlob == null || cred.CredentialBlobSize == 0) return null
            val bytes = cred.CredentialBlob!!.getByteArray(0, cred.CredentialBlobSize)
            return String(bytes, Charsets.UTF_16LE)
        } finally {
            lib.CredFree(pCredential[0])
        }
    }

    override suspend fun write(
        key: String,
        value: String,
    ): Boolean {
        val bytes = value.toByteArray(Charsets.UTF_16LE)
        val blobMemory = Memory(bytes.size.toLong())
        blobMemory.write(0, bytes, 0, bytes.size)

        val cred = CREDENTIAL()
        cred.Flags = 0
        cred.Type = CRED_TYPE_GENERIC
        cred.TargetName = targetName(key)
        cred.CredentialBlobSize = bytes.size
        cred.CredentialBlob = blobMemory
        cred.Persist = CRED_PERSIST_LOCAL_MACHINE
        cred.UserName = null

        return try {
            lib.CredWriteW(cred, 0)
        } catch (_: LastErrorException) {
            false
        }
    }

    override suspend fun exists(key: String): Boolean = read(key) != null

    override suspend fun delete(key: String): Boolean {
        try {
            lib.CredDeleteW(targetName(key), CRED_TYPE_GENERIC, 0)
        } catch (_: LastErrorException) {
            // Deleting a non-existent key is not an error
        }
        return true
    }

    override suspend fun list(): List<String> {
        val count = intArrayOf(0)
        val pCredentials = arrayOfNulls<Pointer>(1)
        val filter = "$TARGET_PREFIX*"

        val success = lib.CredEnumerateW(filter, 0, count, pCredentials)
        if (!success || pCredentials[0] == null || count[0] == 0) return emptyList()

        try {
            val credPointers = pCredentials[0]!!.getPointerArray(0, count[0])
            return credPointers.mapNotNull { ptr ->
                val cred = Structure.newInstance(CREDENTIAL::class.java, ptr)
                cred.read()
                cred.TargetName?.removePrefix(TARGET_PREFIX)
            }
        } finally {
            lib.CredFree(pCredentials[0])
        }
    }

    override suspend fun deleteAll(): Boolean {
        val keys = list()
        var allDeleted = true
        for (key in keys) {
            if (!delete(key)) {
                allDeleted = false
            }
        }
        return allDeleted
    }
}
