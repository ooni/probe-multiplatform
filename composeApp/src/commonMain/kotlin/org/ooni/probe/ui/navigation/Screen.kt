package org.ooni.probe.ui.navigation

sealed class Screen(
    val route: String,
) {
    data object Dashboard : Screen("dashboard")

    data object Results : Screen("results")

    data object Settings : Screen("settings")
}
