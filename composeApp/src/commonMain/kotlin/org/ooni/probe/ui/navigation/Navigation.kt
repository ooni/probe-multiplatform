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
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_SendEmail_Label
import ooniprobe.composeapp.generated.resources.Settings_SendEmail_Message
import ooniprobe.composeapp.generated.resources.shareEmailTo
import ooniprobe.composeapp.generated.resources.shareSubject
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.SettingsCategoryItem
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.decodeUrlFromBase64
import org.ooni.probe.ui.dashboard.DashboardScreen
import org.ooni.probe.ui.measurement.MeasurementScreen
import org.ooni.probe.ui.result.ResultScreen
import org.ooni.probe.ui.results.ResultsScreen
import org.ooni.probe.ui.running.RunningScreen
import org.ooni.probe.ui.settings.SettingsScreen
import org.ooni.probe.ui.settings.about.AboutScreen
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
            val viewModel = viewModel {
                dependencies.dashboardViewModel(
                    goToResults = { navController.navigateToMainScreen(Screen.Results) },
                    goToRunningTest = { navController.navigate(Screen.RunningTest.route) },
                )
            }
            val state by viewModel.state.collectAsState()
            DashboardScreen(state, viewModel::onEvent)
        }

        composable(route = Screen.Results.route) {
            val viewModel = viewModel {
                dependencies.resultsViewModel(
                    goToResult = { navController.navigate(Screen.Result(it).route) },
                )
            }
            val state by viewModel.state.collectAsState()
            ResultsScreen(state, viewModel::onEvent)
        }

        composable(route = Screen.Settings.route) {
            val sendSupportEmail = dependencies.sendSupportEmail()
            val supportEmail = stringResource(Res.string.shareEmailTo)
            val subject = stringResource(Res.string.shareSubject, dependencies.platformInfo.version)
            val chooserTitle = stringResource(Res.string.Settings_SendEmail_Label)
            val platformInfo = dependencies.platformInfo
            val body = stringResource(Res.string.Settings_SendEmail_Message) + "\n\n\n" +
                "PLATFORM: ${platformInfo.platform}\n" +
                "MODEL: ${platformInfo.model}\n" +
                "OS Version: ${platformInfo.osVersion}"
            val viewModel =
                viewModel {
                    dependencies.settingsViewModel(
                        goToSettingsForCategory = {
                            navController.navigate(Screen.SettingsCategory(it).route)
                        },
                        sendSupportEmail = {
                            sendSupportEmail.invoke(
                                supportEmail,
                                mapOf(
                                    "subject" to subject,
                                    "body" to body,
                                    "chooserTitle" to chooserTitle
                                ),
                            )
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
            val viewModel = viewModel {
                dependencies.resultViewModel(
                    resultId = ResultModel.Id(resultId),
                    onBack = { navController.popBackStack() },
                    goToMeasurement = { reportId, input ->
                        navController.navigate(Screen.Measurement(reportId, input).route)
                    },
                )
            }
            val state by viewModel.state.collectAsState()
            ResultScreen(state, viewModel::onEvent)
        }

        composable(
            route = Screen.Measurement.NAV_ROUTE,
            arguments = Screen.Measurement.ARGUMENTS,
        ) { entry ->
            val reportId = entry.arguments?.getString("reportId") ?: return@composable
            val input = entry.arguments?.getString("input").decodeUrlFromBase64()
            MeasurementScreen(
                reportId = MeasurementModel.ReportId(reportId),
                input = input,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.SettingsCategory.NAV_ROUTE,
            arguments = Screen.SettingsCategory.ARGUMENTS,
        ) { entry ->
            val category = entry.arguments?.getString("category") ?: return@composable
            when (category) {
                PreferenceCategoryKey.ABOUT_OONI.name -> {
                    val viewModel =
                        viewModel {
                            dependencies.aboutViewModel(
                                onBack = { navController.navigateUp() },
                            )
                        }
                    AboutScreen(onEvent = viewModel::onEvent)
                }

                else -> {
                    val viewModel = viewModel {
                        dependencies.settingsCategoryViewModel(
                            goToSettingsForCategory = {
                                navController.navigate(Screen.SettingsCategory(it).route)
                            },
                            onBack = { navController.popBackStack() },
                            category = SettingsCategoryItem.getSettingsItem(
                                PreferenceCategoryKey.valueOf(category),
                            ),
                        )
                    }
                    val state by viewModel.state.collectAsState()

                    SettingsCategoryScreen(
                        state = state,
                        onEvent = viewModel::onEvent,
                    )
                }
            }
        }

        composable(route = Screen.RunningTest.route) {
            val viewModel = viewModel {
                dependencies.runningViewModel(
                    onBack = { navController.popBackStack() },
                    goToResults = {
                        navController.popBackStack()
                        navController.navigateToMainScreen(Screen.Results)
                    },
                )
            }
            val state by viewModel.state.collectAsState()
            RunningScreen(state, viewModel::onEvent)
        }
    }
}
