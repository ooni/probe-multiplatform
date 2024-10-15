package org.ooni.probe.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import org.ooni.probe.LocalSnackbarHostState
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.decodeUrlFromBase64
import org.ooni.probe.ui.choosewebsites.ChooseWebsitesScreen
import org.ooni.probe.ui.dashboard.DashboardScreen
import org.ooni.probe.ui.descriptor.DescriptorScreen
import org.ooni.probe.ui.descriptor.add.AddDescriptorScreen
import org.ooni.probe.ui.descriptor.review.ReviewUpdatesScreen
import org.ooni.probe.ui.log.LogScreen
import org.ooni.probe.ui.measurement.MeasurementScreen
import org.ooni.probe.ui.onboarding.OnboardingScreen
import org.ooni.probe.ui.result.ResultScreen
import org.ooni.probe.ui.results.ResultsScreen
import org.ooni.probe.ui.run.RunScreen
import org.ooni.probe.ui.running.RunningScreen
import org.ooni.probe.ui.settings.SettingsScreen
import org.ooni.probe.ui.settings.about.AboutScreen
import org.ooni.probe.ui.settings.category.SettingsCategoryScreen
import org.ooni.probe.ui.settings.proxy.ProxyScreen
import org.ooni.probe.ui.upload.UploadMeasurementsDialog

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
        composable(route = Screen.Onboarding.route) {
            val viewModel = viewModel {
                dependencies.onboardingViewModel(
                    goToDashboard = {
                        navController.popBackStack()
                        navController.navigateToMainScreen(Screen.Dashboard)
                    },
                    goToSettings = {
                        navController.popBackStack()
                        navController.navigateToMainScreen(Screen.Settings)
                    },
                )
            }
            val state by viewModel.state.collectAsState()
            OnboardingScreen(state, viewModel::onEvent)
        }

        composable(route = Screen.Dashboard.route) {
            val viewModel = viewModel {
                dependencies.dashboardViewModel(
                    goToOnboarding = {
                        navController.popBackStack()
                        navController.navigate(Screen.Onboarding.route)
                    },
                    goToResults = { navController.navigateToMainScreen(Screen.Results) },
                    goToRunningTest = { navController.navigate(Screen.RunningTest.route) },
                    goToRunTests = { navController.navigate(Screen.RunTests.route) },
                    goToDescriptor = { descriptorKey ->
                        navController.navigate(Screen.Descriptor(descriptorKey).route)
                    },
                    goToReviewDescriptorUpdates = {
                        navController.navigate(Screen.ReviewUpdates.route)
                    },
                )
            }
            val state by viewModel.state.collectAsState()
            DashboardScreen(state, viewModel::onEvent)
        }

        composable(route = Screen.Results.route) {
            val viewModel = viewModel {
                dependencies.resultsViewModel(
                    goToResult = { navController.navigate(Screen.Result(it).route) },
                    goToUpload = { navController.navigate(Screen.UploadMeasurements().route) },
                )
            }
            val state by viewModel.state.collectAsState()
            ResultsScreen(state, viewModel::onEvent)
        }

        composable(route = Screen.Settings.route) {
            val viewModel = viewModel {
                dependencies.settingsViewModel(
                    goToSettingsForCategory = {
                        navController.navigate(Screen.SettingsCategory(it).route)
                    },
                )
            }
            val state by viewModel.state.collectAsState()
            SettingsScreen(state, viewModel::onEvent)
        }

        composable(
            route = Screen.Result.NAV_ROUTE,
            arguments = Screen.Result.ARGUMENTS,
        ) { entry ->
            val resultId = ResultModel.Id(
                entry.arguments?.getLong("resultId") ?: return@composable,
            )
            val viewModel = viewModel {
                dependencies.resultViewModel(
                    resultId = resultId,
                    onBack = { navController.popBackStack() },
                    goToMeasurement = { reportId, input ->
                        navController.navigate(Screen.Measurement(reportId, input).route)
                    },
                    goToUpload = {
                        navController.navigate(Screen.UploadMeasurements(resultId).route)
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
                PreferenceCategoryKey.ABOUT_OONI.value -> {
                    val viewModel = viewModel {
                        dependencies.aboutViewModel(onBack = { navController.navigateUp() })
                    }
                    AboutScreen(
                        onEvent = viewModel::onEvent,
                        softwareName = viewModel.softwareName,
                        softwareVersion = viewModel.softwareVersion,
                    )
                }

                PreferenceCategoryKey.PROXY.value -> {
                    val viewModel = viewModel {
                        dependencies.proxyViewModel(onBack = { navController.navigateUp() })
                    }
                    val state by viewModel.state.collectAsState()
                    ProxyScreen(state, viewModel::onEvent)
                }

                PreferenceCategoryKey.SEE_RECENT_LOGS.value -> {
                    val viewModel = viewModel {
                        dependencies.logViewModel(onBack = { navController.popBackStack() })
                    }
                    val state by viewModel.state.collectAsState()
                    LogScreen(state, viewModel::onEvent)
                }

                else -> {
                    val viewModel = viewModel {
                        dependencies.settingsCategoryViewModel(
                            categoryKey = category,
                            goToSettingsForCategory = {
                                navController.navigate(Screen.SettingsCategory(it).route)
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    val state by viewModel.state.collectAsState()
                    SettingsCategoryScreen(state, viewModel::onEvent)
                }
            }
        }

        composable(route = Screen.RunTests.route) {
            val viewModel = viewModel {
                dependencies.runViewModel(onBack = { navController.popBackStack() })
            }
            val state by viewModel.state.collectAsState()
            RunScreen(state, viewModel::onEvent)
        }

        composable(
            route = Screen.AddDescriptor.NAV_ROUTE,
            arguments = Screen.AddDescriptor.ARGUMENTS,
        ) { entry ->
            entry.arguments?.getLong("runId")?.let { descriptorId ->
                val viewModel = viewModel {
                    dependencies.addDescriptorViewModel(
                        onBack = { navController.popBackStack() },
                        descriptorId = descriptorId.toString(),
                    )
                }
                val state by viewModel.state.collectAsState()
                AddDescriptorScreen(state, viewModel::onEvent)
            } ?: run {
                val snackbarHostState = LocalSnackbarHostState.current
                LaunchedEffect(Unit) {
                    snackbarHostState?.showSnackbar("Invalid descriptor ID")
                }
                navController.popBackStack()
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

        dialog(
            route = Screen.UploadMeasurements.NAV_ROUTE,
            arguments = Screen.UploadMeasurements.ARGUMENTS,
            dialogProperties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        ) { entry ->
            val resultId = entry.arguments?.getString("resultId")?.toLongOrNull()
                ?.let(ResultModel::Id)
            val viewModel = viewModel {
                dependencies.uploadMeasurementsViewModel(
                    resultId = resultId,
                    onClose = { navController.popBackStack() },
                )
            }
            val state by viewModel.state.collectAsState()
            UploadMeasurementsDialog(state, viewModel::onEvent)
        }

        composable(
            route = Screen.Descriptor.NAV_ROUTE,
            arguments = Screen.Descriptor.ARGUMENTS,
        ) { entry ->
            val descriptorKey = entry.arguments?.getString("descriptorKey") ?: return@composable
            val viewModel = viewModel {
                dependencies.descriptorViewModel(
                    descriptorKey = descriptorKey,
                    onBack = { navController.popBackStack() },
                    goToReviewDescriptorUpdates = {
                        navController.navigate(Screen.ReviewUpdates.route)
                    },
                    goToChooseWebsites = { navController.navigate(Screen.ChooseWebsites.route) },
                )
            }
            val state by viewModel.state.collectAsState()
            DescriptorScreen(state, viewModel::onEvent)
        }

        composable(route = Screen.ReviewUpdates.route) { entry ->
            val viewModel = viewModel {
                dependencies.reviewUpdatesViewModel(
                    onBack = { navController.popBackStack() },
                )
            }
            val state by viewModel.state.collectAsState()
            ReviewUpdatesScreen(state, viewModel::onEvent)
        }

        composable(route = Screen.ChooseWebsites.route) { entry ->
            val viewModel = viewModel {
                dependencies.chooseWebsitesViewModel(
                    onBack = { navController.popBackStack() },
                    goToDashboard = {
                        navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                    },
                )
            }
            val state by viewModel.state.collectAsState()
            ChooseWebsitesScreen(state, viewModel::onEvent)
        }
    }
}
