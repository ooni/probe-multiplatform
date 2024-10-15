package org.ooni.probe.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
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

private val START_SCREEN = Screen.Dashboard

@Composable
fun Navigation(
    navController: NavHostController,
    dependencies: Dependencies,
) {
    NavHost(
        navController = navController,
        startDestination = START_SCREEN.route,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(route = Screen.Onboarding.route) {
            val viewModel = viewModel {
                dependencies.onboardingViewModel(
                    goToDashboard = {
                        navController.goBackAndNavigateToMain(Screen.Dashboard)
                    },
                    goToSettings = {
                        navController.goBackAndNavigateToMain(Screen.Settings)
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
                        navController.goBackAndNavigate(Screen.Onboarding)
                    },
                    goToResults = { navController.navigateToMainScreen(Screen.Results) },
                    goToRunningTest = { navController.safeNavigate(Screen.RunningTest) },
                    goToRunTests = { navController.safeNavigate(Screen.RunTests) },
                    goToDescriptor = { descriptorKey ->
                        navController.safeNavigate(Screen.Descriptor(descriptorKey))
                    },
                    goToReviewDescriptorUpdates = {
                        navController.safeNavigate(Screen.ReviewUpdates)
                    },
                )
            }
            val state by viewModel.state.collectAsState()
            DashboardScreen(state, viewModel::onEvent)
        }

        composable(route = Screen.Results.route) {
            val viewModel = viewModel {
                dependencies.resultsViewModel(
                    goToResult = { navController.safeNavigate(Screen.Result(it)) },
                    goToUpload = { navController.safeNavigate(Screen.UploadMeasurements()) },
                )
            }
            val state by viewModel.state.collectAsState()
            ResultsScreen(state, viewModel::onEvent)
        }

        composable(route = Screen.Settings.route) {
            val viewModel = viewModel {
                dependencies.settingsViewModel(
                    goToSettingsForCategory = {
                        navController.safeNavigate(Screen.SettingsCategory(it))
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
                    onBack = { navController.goBack() },
                    goToMeasurement = { reportId, input ->
                        navController.safeNavigate(Screen.Measurement(reportId, input))
                    },
                    goToUpload = {
                        navController.safeNavigate(Screen.UploadMeasurements(resultId))
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
                onBack = { navController.goBack() },
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
                        dependencies.aboutViewModel(onBack = { navController.goBack() })
                    }
                    AboutScreen(
                        onEvent = viewModel::onEvent,
                        softwareName = viewModel.softwareName,
                        softwareVersion = viewModel.softwareVersion,
                    )
                }

                PreferenceCategoryKey.PROXY.value -> {
                    val viewModel = viewModel {
                        dependencies.proxyViewModel(onBack = { navController.goBack() })
                    }
                    val state by viewModel.state.collectAsState()
                    ProxyScreen(state, viewModel::onEvent)
                }

                PreferenceCategoryKey.SEE_RECENT_LOGS.value -> {
                    val viewModel = viewModel {
                        dependencies.logViewModel(onBack = { navController.goBack() })
                    }
                    val state by viewModel.state.collectAsState()
                    LogScreen(state, viewModel::onEvent)
                }

                else -> {
                    val viewModel = viewModel {
                        dependencies.settingsCategoryViewModel(
                            categoryKey = category,
                            goToSettingsForCategory = {
                                navController.safeNavigate(Screen.SettingsCategory(it))
                            },
                            onBack = { navController.goBack() },
                        )
                    }
                    val state by viewModel.state.collectAsState()
                    SettingsCategoryScreen(state, viewModel::onEvent)
                }
            }
        }

        composable(route = Screen.RunTests.route) {
            val viewModel = viewModel {
                dependencies.runViewModel(onBack = { navController.goBack() })
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
                        onBack = { navController.goBack() },
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
                navController.goBack()
            }
        }

        composable(route = Screen.RunningTest.route) {
            val viewModel = viewModel {
                dependencies.runningViewModel(
                    onBack = { navController.goBack() },
                    goToResults = {
                        navController.goBackAndNavigateToMain(Screen.Results)
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
                    onClose = { navController.goBack() },
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
                    onBack = { navController.goBack() },
                    goToReviewDescriptorUpdates = {
                        navController.safeNavigate(Screen.ReviewUpdates)
                    },
                    goToChooseWebsites = { navController.navigate(Screen.ChooseWebsites.route) },
                )
            }
            val state by viewModel.state.collectAsState()
            DescriptorScreen(state, viewModel::onEvent)
        }

        composable(route = Screen.ReviewUpdates.route) {
            val viewModel = viewModel {
                dependencies.reviewUpdatesViewModel(
                    onBack = { navController.goBack() },
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

// Helpers

private fun NavController.goBack() {
    if (!isResumed()) return
    if (!popBackStack()) {
        navigateToMainScreen(START_SCREEN)
    }
}

private fun NavController.goBackTo(
    screen: Screen,
    inclusive: Boolean = false,
) {
    if (!isResumed()) return
    if (!popBackStack(screen.route, inclusive = inclusive)) {
        navigateToMainScreen(START_SCREEN)
    }
}

private fun NavController.goBackAndNavigate(screen: Screen) {
    if (!isResumed()) return
    popBackStack()
    navigate(screen.route)
}

private fun NavController.goBackAndNavigateToMain(screen: Screen) {
    if (!isResumed()) return
    popBackStack()
    navigateToMainScreen(screen)
}

private fun NavController.safeNavigate(screen: Screen) {
    if (!isResumed()) return
    navigate(screen.route)
}

fun NavController.safeNavigateToMain(screen: Screen) {
    if (!isResumed()) return
    navigateToMainScreen(screen)
}

private fun NavController.isResumed() = currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

private fun NavController.navigateToMainScreen(screen: Screen) {
    navigate(screen.route) {
        // Pop up to the start destination of the graph to
        // avoid building up a large stack of destinations
        // on the back stack as users select items
        graph.findStartDestination().route?.let {
            popUpTo(it) {
                saveState = true
            }
        }
        // Avoid multiple copies of the same destination when
        // re-selecting the same item
        launchSingleTop = true
        // Restore state when re-selecting a previously selected item
        restoreState = true
    }
}
