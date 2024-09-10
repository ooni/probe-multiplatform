package org.ooni.probe.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.shared.encodeUrlToBase64

sealed class Screen(
    val route: String,
) {
    data object Dashboard : Screen("dashboard")

    data object Results : Screen("results")

    data object Settings : Screen("settings")

    data class Result(
        val resultId: ResultModel.Id,
    ) : Screen("results/${resultId.value}") {
        companion object {
            const val NAV_ROUTE = "results/{resultId}"
            val ARGUMENTS = listOf(navArgument("resultId") { type = NavType.LongType })
        }
    }

    data class AddDescriptor(
        val runId: String,
    ) : Screen("add-descriptor/$runId") {
        companion object {
            const val NAV_ROUTE = "add-descriptor/{runId}"
            val ARGUMENTS = listOf(navArgument("runId") { type = NavType.LongType })
        }
    }

    data class Measurement(
        val measurementReportId: MeasurementModel.ReportId,
        val input: String?,
    ) : Screen("measurements/${measurementReportId.value}?input=${input.encodeUrlToBase64()}") {
        companion object {
            const val NAV_ROUTE = "measurements/{reportId}?input={input}"
            val ARGUMENTS = listOf(
                navArgument("reportId") { type = NavType.StringType },
                navArgument("input") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                },
            )
        }
    }

    data class SettingsCategory(
        val category: PreferenceCategoryKey,
    ) : Screen("settings/${category.value}") {
        companion object {
            const val NAV_ROUTE = "settings/{category}"
            val ARGUMENTS = listOf(navArgument("category") { type = NavType.StringType })
        }
    }

    data object RunTests : Screen("run")

    data object RunningTest : Screen("running")

    data object UploadMeasurements : Screen("upload")

    data class Descriptor(
        val descriptorKey: String,
    ) : Screen("descriptors/$descriptorKey") {
        companion object {
            const val NAV_ROUTE = "descriptors/{descriptorKey}"
            val ARGUMENTS = listOf(navArgument("descriptorKey") { type = NavType.StringType })
        }
    }
}
