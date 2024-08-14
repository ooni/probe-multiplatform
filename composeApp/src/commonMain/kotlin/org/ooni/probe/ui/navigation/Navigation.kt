package org.ooni.probe.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.ooni.probe.data.models.ResultModel
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.send_email
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.di.Dependencies
import org.ooni.probe.ui.dashboard.DashboardScreen
import org.ooni.probe.ui.result.ResultScreen
import org.ooni.probe.ui.results.ResultsScreen
import org.ooni.probe.ui.settings.SettingsScreen
import org.ooni.probe.ui.settings.category.SettingsCategoryScreen

@Composable
fun Navigation(
    navController: NavHostController,
    dependencies: Dependencies,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(route = Screen.Dashboard.route) {
            val viewModel = viewModel { dependencies.dashboardViewModel }
            val state by viewModel.state.collectAsState()
            DashboardScreen(state, viewModel::onEvent)
        }

        composable(route = Screen.Results.route) {
            val viewModel =
                viewModel {
                    dependencies.resultsViewModel(
                        goToResult = { navController.navigate(Screen.Result(it).route) },
                    )
                }
            val state by viewModel.state.collectAsState()
            ResultsScreen(state, viewModel::onEvent)
        }

        composable(route = Screen.Settings.route) {
            val viewModel =
                viewModel {
                    dependencies.settingsViewModel(
                        goToSettingsForCategory = {
                            navController.navigate(Screen.SettingsCategory(it).route)
                        },
                    )
                }
            SettingsScreen(viewModel::onEvent)
        }

        composable(
            route = Screen.Result.NAV_ROUTE,
            arguments = Screen.Result.ARGUMENTS,
        ) { entry ->
            val resultId = entry.arguments?.getLong("resultId") ?: return@composable
            val viewModel =
                viewModel {
                    dependencies.resultViewModel(
                        resultId = ResultModel.Id(resultId),
                        onBack = { navController.navigateUp() },
                    )
                }
            val state by viewModel.state.collectAsState()
            ResultScreen(state, viewModel::onEvent)
        }

        composable(
            route = Screen.SettingsCategory.NAV_ROUTE,
            arguments = Screen.SettingsCategory.ARGUMENTS,
        ) { entry ->
            val category = entry.arguments?.getString("category") ?: return@composable
            when (category) {
                stringResource(Res.string.send_email) -> {
                    // TODO: Implement based on platform
                }

                else -> {
                    val viewModel =
                        viewModel {
                            dependencies.settingsCategoryViewModel(
                                goToSettingsForCategory = {
                                    navController.navigate(Screen.SettingsCategory(it).route)
                                },
                                onBack = { navController.navigateUp() },
                            )
                        }
                    SettingsCategoryScreen(category = category, onEvent = viewModel::onEvent)
                }
            }
        }
    }
}
