package org.ooni.probe.domain.descriptors

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.ooni.probe.data.models.DescriptorsUpdateState

class DescriptorUpdateStateManager {
    private val state = MutableStateFlow(DescriptorsUpdateState())

    fun observe() = state.asStateFlow()

    fun update(updateCall: (DescriptorsUpdateState) -> DescriptorsUpdateState) {
        state.update { updateCall(it) }
    }
}
