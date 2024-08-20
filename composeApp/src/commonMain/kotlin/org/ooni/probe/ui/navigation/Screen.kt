package org.ooni.probe.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.ResultModel

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

    data class SettingsCategory(val category: PreferenceCategoryKey) : Screen("settings/${category.name}") {
        companion object {
            const val NAV_ROUTE = "settings/{category}"
            val ARGUMENTS = listOf(navArgument("category") { type = NavType.StringType })
        }
    }
}
