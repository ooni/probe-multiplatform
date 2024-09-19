package org.ooni.probe.data.models

/**
 * Enum representing the type of progress to be displayed in the [UpdateStatus] view.
 */
enum class UpdateStatusType {
    None,
    FetchingUpdates,
    UpdateLink,
    ReviewLink,
}
