package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.coroutines.CoroutineContext

class GetStorageUsed(
    private val backgroundContext: CoroutineContext,
    private val baseFileDir: String,
    private val fileSystem: FileSystem,
    private val cacheDir: String,
) {
    private val storageUsed = MutableStateFlow<Long>(0)

    fun observe(): Flow<Long> =
        storageUsed.asStateFlow()
            .onStart { if (storageUsed.value == 0L) update() }

    suspend fun update(): Long {
        return withContext(backgroundContext) {
            val usedStorage =
                (
                    fileSystem.listRecursively(baseFileDir.toPath()).toList() + fileSystem.listRecursively(
                        cacheDir.toPath(),
                    )
                ).filter { fileSystem.metadata(it).isRegularFile }
                    .sumOf { (fileSystem.metadata(it).size ?: 0) }
            storageUsed.update { usedStorage }
            return@withContext usedStorage
        }
    }
}
