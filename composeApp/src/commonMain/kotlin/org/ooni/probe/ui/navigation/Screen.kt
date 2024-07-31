package org.ooni.probe.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument
import org.ooni.probe.data.models.TestResult

sealed class Screen(
    val route: String,
) {
    data object Dashboard : Screen("dashboard")

    data object Results : Screen("results")

    data object Settings : Screen("settings")

    data class Result(val resultId: TestResult.Id) : Screen("results/${resultId.value}") {
        companion object {
            const val NAV_ROUTE = "results/{resultId}"
            val ARGUMENTS = listOf(navArgument("resultId") { type = NavType.StringType })
        }
    }
}
