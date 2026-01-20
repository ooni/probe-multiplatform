package org.ooni.probe.config

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface LegacyDirectoryManager {
    val isCleanUpRequired: Flow<Boolean> get() = flowOf(false)

    suspend fun cleanUp(): Boolean = false
}
