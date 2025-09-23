package org.ooni.probe.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable data object Onboarding : Screen

    @Serializable data object Dashboard : Screen

    @Serializable data object Results : Screen

    @Serializable data object Settings : Screen

    @Serializable data class Result(
        val resultId: Long,
    ) : Screen

    @Serializable data class AddDescriptor(
        val runId: Long,
    ) : Screen

    @Serializable data class Measurement(
        val measurementId: Long,
    ) : Screen

    @Serializable data class MeasurementRaw(
        val measurementId: Long,
    ) : Screen

    @Serializable data class SettingsCategory(
        val category: String,
    ) : Screen

    @Serializable data object RunTests : Screen

    @Serializable data object RunningTest : Screen

    @Serializable data class UploadMeasurements(
        val resultId: Long? = null,
        val measurementId: Long? = null,
    ) : Screen

    @Serializable data class ChooseWebsites(
        val url: String? = null,
    ) : Screen

    @Serializable data class Descriptor(
        val descriptorKey: String,
    ) : Screen

    @Serializable data class DescriptorWebsites(
        val descriptorId: String,
    ) : Screen

    @Serializable data class ReviewUpdates(
        val descriptorIds: List<String>? = null,
    ) : Screen
}
