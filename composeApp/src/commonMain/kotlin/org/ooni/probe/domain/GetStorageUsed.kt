package org.ooni.probe.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okio.FileSystem
import okio.Path.Companion.toPath

class GetStorageUsed(
    private val baseFileDir: String,
    private val fileSystem: FileSystem,
    private val cacheDir: String,
) {
    private val storageUsed = MutableStateFlow<Long>(0)

    fun observe(): StateFlow<Long> {
        if (storageUsed.value == 0L) {
            update()
        }
        return storageUsed.asStateFlow()
    }

    fun update(): Long {
        val usedStorage =
            (
                fileSystem.listRecursively(baseFileDir.toPath()).toList() + fileSystem.listRecursively(
                    cacheDir.toPath(),
                )
            ).filter { fileSystem.metadata(it).isRegularFile }
                .sumOf { (fileSystem.metadata(it).size ?: 0) }
        storageUsed.update { usedStorage }
        return usedStorage
    }
}
