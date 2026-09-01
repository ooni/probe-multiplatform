package org.ooni.engine

interface SecureStorage {
    suspend fun read(key: String): String?

    suspend fun write(
        key: String,
        value: String,
    ): WriteResult

    suspend fun exists(key: String): Boolean

    suspend fun delete(key: String): DeleteResult

    suspend fun list(): List<String>

    suspend fun deleteAll(): DeleteAllResult
}
