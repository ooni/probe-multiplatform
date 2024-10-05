package org.ooni.probe.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GetStorageUsed(
    private val osStorageUsed: () -> Long,
) {
    private val _storageUsed = MutableStateFlow<Long>(0)
    val storageUsed: StateFlow<Long> = _storageUsed.asStateFlow()

    operator fun invoke() {
        _storageUsed.update { osStorageUsed() }
    }
}
