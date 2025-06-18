package org.ooni.probe.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import androidx.navigation.toRoute
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.di.Dependencies
import org.ooni.probe.ui.choosewebsites.ChooseWebsitesScreen
import org.ooni.probe.ui.dashboard.DashboardScreen
import org.ooni.probe.ui.descriptor.DescriptorScreen
import org.ooni.probe.ui.descriptor.add.AddDescriptorScreen
import org.ooni.probe.ui.descriptor.review.ReviewUpdatesScreen
import org.ooni.probe.ui.log.LogScreen
import org.ooni.probe.ui.measurement.MeasurementRawScreen
import org.ooni.probe.ui.measurement.MeasurementScreen
import org.ooni.probe.ui.onboarding.OnboardingScreen
import org.ooni.probe.ui.result.ResultScreen
import org.ooni.probe.ui.results.ResultsScreen
import org.ooni.probe.ui.run.RunScreen
import org.ooni.probe.ui.running.RunningScreen
import org.ooni.probe.ui.settings.SettingsScreen
import org.ooni.probe.ui.settings.about.AboutScreen
import org.ooni.probe.ui.settings.category.SettingsCategoryScreen
import org.ooni.probe.ui.settings.donate.DonateScreen
import org.ooni.probe.ui.settings.proxy.ProxyScreen
import org.ooni.probe.ui.settings.webcategories.WebCategoriesScreen
import org.ooni.probe.ui.upload.UploadMeasurementsDialog

private val START_SCREEN = Screen.Dashboard

@Composable
fun Navigation(
    navController: NavHostController,
    dependencies: Dependencies,
) {
    NavHost(
        navController = navController,
        startDestination = START_SCREEN,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable<Screen.Onboarding> {
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

        composable<Screen.Dashboard> {
            val viewModel = viewModel {
                dependencies.dashboardViewModel(
                    goToOnboarding = { navController.goBackAndNavigate(Screen.Onboarding) },
                    goToResults = { navController.navigateToMainScreen(Screen.Results) },
                    goToRunningTest = { navController.safeNavigate(Screen.RunningTest) },
                    goToRunTests = { navController.safeNavigate(Screen.RunTests()) },
                    goToDescriptor = { descriptorKey ->
                        navController.safeNavigate(Screen.Descriptor(descriptorKey))
                    },
                    goToReviewDescriptorUpdates = { list ->
                        navController.safeNavigate(Screen.ReviewUpdates(list?.map { it.value }))
                    },
                )
            }
            val state by viewModel.state.collectAsState()
            DashboardScreen(state, viewModel::onEvent)
        }

        composable<Screen.Results> {
            val viewModel = viewModel {
                dependencies.resultsViewModel(
                    goToResult = { navController.safeNavigate(Screen.Result(it.value)) },
                    goToUpload = { navController.safeNavigate(Screen.UploadMeasurements()) },
                )
            }
            val state by viewModel.state.collectAsState()
            ResultsScreen(state, viewModel::onEvent)
        }

        composable<Screen.Settings> {
            val viewModel = viewModel {
                dependencies.settingsViewModel(
                    goToSettingsForCategory = {
                        navController.safeNavigate(Screen.SettingsCategory(it.value))
                    },
                )
            }
            val state by viewModel.state.collectAsState()
            SettingsScreen(state, viewModel::onEvent)
        }

        composable<Screen.Result> { entry ->
            val resultId = entry.toRoute<Screen.Result>().resultId
            val viewModel = viewModel {
                dependencies.resultViewModel(
                    resultId = ResultModel.Id(resultId),
                    onBack = { navController.goBack() },
                    goToMeasurement = { reportId, input ->
                        navController.safeNavigate(Screen.Measurement(reportId.value, input))
                    },
                    goToMeasurementRaw = {
                        navController.safeNavigate(Screen.MeasurementRaw(it.value))
                    },
                    goToUpload = { navController.safeNavigate(Screen.UploadMeasurements(resultId)) },
                    goToDashboard = {
                        navController.popBackStack(Screen.Dashboard, inclusive = false)
                    },
                )
            }
            val state by viewModel.state.collectAsState()
            ResultScreen(state, viewModel::onEvent)
        }

        composable<Screen.Measurement> { entry ->
            val route = entry.toRoute<Screen.Measurement>()
            val viewModel = viewModel {
                dependencies.measurementViewModel(
                    reportId = MeasurementModel.ReportId(route.measurementReportId),
                    input = route.input,
                    onBack = { navController.goBack() },
                )
            }
            val state by viewModel.state.collectAsState()
            MeasurementScreen(state, viewModel::onEvent)
        }

        composable<Screen.MeasurementRaw> { entry ->
            val route = entry.toRoute<Screen.MeasurementRaw>()
            val viewModel = viewModel {
                dependencies.measurementRawViewModel(
                    measurementId = MeasurementModel.Id(route.measurementId),
                    onBack = { navController.goBack() },
                    goToUpload = {
                        navController.safeNavigate(Screen.UploadMeasurements(measurementId = it.value))
                    },
                    goToMeasurement = { reportId, input ->
                        navController.goBackAndNavigate(Screen.Measurement(reportId.value, input))
                    },
                )
            }
            val state by viewModel.state.collectAsState()
            MeasurementRawScreen(state, viewModel::onEvent)
        }

        composable<Screen.SettingsCategory> { entry ->
            val categoryKey = entry.toRoute<Screen.SettingsCategory>().category
            val category = PreferenceCategoryKey.fromValue(categoryKey) ?: return@composable
            when (category) {
                PreferenceCategoryKey.WEBSITES_CATEGORIES -> {
                    val viewModel = viewModel {
                        dependencies.webCategoriesViewModel(onBack = { navController.goBack() })
                    }
                    val state by viewModel.state.collectAsState()
                    WebCategoriesScreen(state, viewModel::onEvent)
                }

                PreferenceCategoryKey.ABOUT_OONI -> {
                    val viewModel = viewModel {
                        dependencies.aboutViewModel(onBack = { navController.goBack() })
                    }
                    AboutScreen(
                        onEvent = viewModel::onEvent,
                        softwareName = viewModel.softwareName,
                        softwareVersion = viewModel.softwareVersion,
                    )
                }

                PreferenceCategoryKey.DONATE -> {
                    DonateScreen(
                        onBack = { navController.goBack() },
                        openUrl = { dependencies.launchAction(PlatformAction.OpenUrl(it)) },
                    )
                }

                PreferenceCategoryKey.PROXY -> {
                    val viewModel = viewModel {
                        dependencies.proxyViewModel(onBack = { navController.goBack() })
                    }
                    val state by viewModel.state.collectAsState()
                    ProxyScreen(state, viewModel::onEvent)
                }

                PreferenceCategoryKey.SEE_RECENT_LOGS -> {
                    val viewModel = viewModel {
                        dependencies.logViewModel(onBack = { navController.goBack() })
                    }
                    val state by viewModel.state.collectAsState()
                    LogScreen(state, viewModel::onEvent)
                }

                else -> {
                    val viewModel = viewModel {
                        dependencies.settingsCategoryViewModel(
                            categoryKey = category.value,
                            goToSettingsForCategory = {
                                navController.safeNavigate(Screen.SettingsCategory(it.value))
                            },
                            onBack = { navController.goBack() },
                        )
                    }
                    val state by viewModel.state.collectAsState()
                    SettingsCategoryScreen(state, viewModel::onEvent)
                }
            }
        }

        composable<Screen.RunTests> { entry ->
            val descriptorKey = entry.toRoute<Screen.RunTests>().descriptorKey
            val viewModel = viewModel {
                dependencies.runViewModel(
                    descriptorKey = descriptorKey,
                    onBack = { navController.goBack() },
                )
            }
            val state by viewModel.state.collectAsState()
            RunScreen(state, viewModel::onEvent)
        }

        composable<Screen.AddDescriptor> { entry ->
            val descriptorId = entry.toRoute<Screen.AddDescriptor>().runId
            val viewModel = viewModel {
                dependencies.addDescriptorViewModel(
                    onBack = { navController.goBack() },
                    descriptorId = descriptorId.toString(),
                )
            }
            val state by viewModel.state.collectAsState()
            AddDescriptorScreen(state, viewModel::onEvent)
        }

        composable<Screen.RunningTest> {
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

        dialog<Screen.UploadMeasurements>(
            dialogProperties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        ) { entry ->
            val route = entry.toRoute<Screen.UploadMeasurements>()
            val filter = route.resultId?.let {
                MeasurementsFilter.Result(ResultModel.Id(it))
            }
                ?: route.measurementId?.let {
                    MeasurementsFilter.Measurement(MeasurementModel.Id(it))
                }
                ?: MeasurementsFilter.All
            val viewModel = viewModel {
                dependencies.uploadMeasurementsViewModel(
                    filter = filter,
                    onClose = { navController.goBack() },
                )
            }
            val state by viewModel.state.collectAsState()
            UploadMeasurementsDialog(state, viewModel::onEvent)
        }

        composable<Screen.Descriptor> { entry ->
            val viewModel = viewModel {
                dependencies.descriptorViewModel(
                    descriptorKey = entry.toRoute<Screen.Descriptor>().descriptorKey,
                    onBack = { navController.goBack() },
                    goToReviewDescriptorUpdates = { list ->
                        navController.safeNavigate(Screen.ReviewUpdates(list?.map { it.value }))
                    },
                    goToRun = { navController.safeNavigate(Screen.RunTests(it)) },
                    goToChooseWebsites = { navController.safeNavigate(Screen.ChooseWebsites()) },
                    goToResult = { navController.safeNavigate(Screen.Result(it.value)) },
                )
            }
            val state by viewModel.state.collectAsState()
            DescriptorScreen(state, viewModel::onEvent)
        }

        composable<Screen.ReviewUpdates> { entry ->
            val viewModel = viewModel {
                dependencies.reviewUpdatesViewModel(
                    descriptorIds = entry.toRoute<Screen.ReviewUpdates>().descriptorIds
                        ?.map(InstalledTestDescriptorModel::Id),
                    onBack = { navController.goBack() },
                )
            }
            val state by viewModel.state.collectAsState()
            ReviewUpdatesScreen(state, viewModel::onEvent)
        }

        composable<Screen.ChooseWebsites> { entry ->
            val viewModel = viewModel {
                dependencies.chooseWebsitesViewModel(
                    onBack = { navController.goBack() },
                    initialUrl = entry.toRoute<Screen.ChooseWebsites>().url,
                    goToDashboard = {
                        navController.goBackTo(Screen.Dashboard, inclusive = false)
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
    if (!isStarted()) return
    if (!popBackStack()) {
        navigateToMainScreen(START_SCREEN)
    }
}

private fun NavController.goBackTo(
    screen: Screen,
    inclusive: Boolean = false,
) {
    if (!isStarted()) return
    if (!popBackStack(screen, inclusive = inclusive)) {
        navigateToMainScreen(START_SCREEN)
    }
}

private fun NavController.goBackAndNavigate(screen: Screen) {
    if (!isStarted()) return
    popBackStack()
    navigate(screen)
}

private fun NavController.goBackAndNavigateToMain(screen: Screen) {
    if (!isStarted()) return
    popBackStack()
    navigateToMainScreen(screen)
}

fun NavController.safeNavigate(screen: Screen) {
    if (!isStarted()) return
    navigate(screen)
}

private fun NavController.isStarted() =
    setOf(Lifecycle.State.STARTED, Lifecycle.State.RESUMED)
        .contains(currentBackStackEntry?.lifecycle?.currentState)

fun NavController.navigateToMainScreen(screen: Screen) {
    navigate(screen) {
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
