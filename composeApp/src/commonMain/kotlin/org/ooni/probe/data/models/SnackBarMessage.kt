package org.ooni.probe.data.models

sealed interface SnackBarMessage {
    data object AddDescriptorFailed : SnackBarMessage

    data object AddDescriptorCancel : SnackBarMessage

    data object AddDescriptorSuccess : SnackBarMessage
}
