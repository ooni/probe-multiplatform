package org.ooni.probe.domain.descriptors

import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.DescriptorUpdateOperationState

class DismissDescriptorReviewNotice(
    private val updateState: ((DescriptorsUpdateState) -> DescriptorsUpdateState) -> Unit,
) {
    operator fun invoke() {
        updateState {
            it.copy(operationState = DescriptorUpdateOperationState.Idle)
        }
    }
}
