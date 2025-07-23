package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okio.FileNotFoundException
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
        storageUsed
            .asStateFlow()
            .onStart { update() }

    suspend fun update(): Long {
        return withContext(backgroundContext) {
            val allFiles = listOf(baseFileDir, cacheDir)
                .flatMap { fileSystem.listRecursively(baseFileDir.toPath()) }
            val usedStorage = allFiles.sumOf {
                val metadata = try {
                    fileSystem.metadata(it)
                } catch (e: FileNotFoundException) {
                    return@sumOf 0L
                }
                if (metadata.isRegularFile) metadata.size ?: 0 else 0
            }
            storageUsed.update { usedStorage }
            return@withContext usedStorage
        }
    }
}
