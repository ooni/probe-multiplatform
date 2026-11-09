package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okio.FileNotFoundException
import okio.FileSystem
import okio.IOException
import okio.Path
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
                .flatMap { listFiles(it) }
            val usedStorage = allFiles.sumOf {
                val metadata = try {
                    fileSystem.metadata(it)
                } catch (_: FileNotFoundException) {
                    return@sumOf 0L
                }
                if (metadata.isRegularFile) metadata.size ?: 0 else 0
            }
            storageUsed.update { usedStorage }
            return@withContext usedStorage
        }
    }

    private fun listFiles(folder: String): List<Path> =
        try {
            fileSystem.listRecursively(folder.toPath()).toList()
        } catch (e: IOException) {
            Logger.w("Could not list files from $folder", e)
            emptyList()
        }
}
