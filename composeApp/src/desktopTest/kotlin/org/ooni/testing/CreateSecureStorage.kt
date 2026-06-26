package org.ooni.testing

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ooni.engine.DeleteAllResult
import org.ooni.engine.DeleteResult
import org.ooni.engine.SecureStorage
import org.ooni.engine.WriteResult

internal actual fun createTestSecureStorage(): SecureStorage = InMemorySecureStorage()

/**
 * In-memory [SecureStorage] for tests: a plain map guarded by a [Mutex], with the same
 * Created/Updated and Deleted/NotFound semantics as the real desktop backends.
 */
class InMemorySecureStorage : SecureStorage {
    private val mutex = Mutex()
    private val store = mutableMapOf<String, String>()

    override suspend fun read(key: String): String? = mutex.withLock { store[key] }

    override suspend fun write(
        key: String,
        value: String,
    ): WriteResult =
        mutex.withLock {
            val existed = store.put(key, value) != null
            if (existed) WriteResult.Updated(key) else WriteResult.Created(key)
        }

    override suspend fun exists(key: String): Boolean = mutex.withLock { store.containsKey(key) }

    override suspend fun delete(key: String): DeleteResult =
        mutex.withLock {
            if (store.remove(key) != null) DeleteResult.Deleted(key) else DeleteResult.NotFound(key)
        }

    override suspend fun list(): List<String> = mutex.withLock { store.keys.toList() }

    override suspend fun deleteAll(): DeleteAllResult =
        mutex.withLock {
            val count = store.size
            store.clear()
            DeleteAllResult.DeletedCount(count)
        }
}
