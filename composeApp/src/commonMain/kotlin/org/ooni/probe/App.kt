package org.ooni.probe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import ooniprobe.composeapp.generated.resources.AddDescriptor_Toasts_Unsupported_Url
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.getString
import org.ooni.probe.data.models.DeepLink
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.ui.navigation.BottomBarViewModel
import org.ooni.probe.ui.navigation.BottomNavigationBar
import org.ooni.probe.ui.navigation.Navigation
import org.ooni.probe.ui.navigation.Screen
import org.ooni.probe.ui.theme.AppTheme

@Composable
fun App(
    dependencies: Dependencies,
    deepLink: DeepLink?,
    onDeeplinkHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentNavEntry by navController.currentBackStackEntryAsState()
    val isMainScreen = MAIN_NAVIGATION_SCREENS.any {
        currentNavEntry?.destination?.hasRoute(it::class) == true
    }

    CompositionLocalProvider(
        values = dependencies.localeDirection
            ?.invoke()
            ?.let { LocalLayoutDirection provides it }
            ?.let {
                arrayOf(LocalSnackbarHostState provides snackbarHostState, it)
            } ?: arrayOf(LocalSnackbarHostState provides snackbarHostState),
    ) {
        AppTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (isMainScreen) {
                            val bottomBarViewModel: BottomBarViewModel = viewModel {
                                dependencies.bottomBarViewModel()
                            }
                            val bottomBarState by bottomBarViewModel.state.collectAsState()

                            BottomNavigationBar(
                                state = bottomBarState,
                                navController = navController,
                            )
                        }
                    },
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .run {
                                if (isMainScreen) {
                                    padding(bottom = paddingValues.calculateBottomPadding())
                                } else {
                                    this
                                }
                            },
                    ) {
                        Navigation(
                            navController = navController,
                            dependencies = dependencies,
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        Logger.setMinSeverity(Severity.Verbose)
        Logger.addLogWriter(dependencies.crashMonitoring.logWriter)
        Logger.addLogWriter(dependencies.appLogger.logWriter)
        logAppStart(dependencies.platformInfo)
        dependencies.appLogger.writeLogsToFile()
    }
    LaunchedEffect(Unit) {
        if (dependencies.flavorConfig.isCrashReportingEnabled) {
            dependencies.crashMonitoring.setup()
        }
    }
    LaunchedEffect(Unit) {
        dependencies.bootstrapTestDescriptors()
        dependencies.bootstrapPreferences()
        // Disabling starting a RunWorker at app start to check if it fixes the
        // ForegroundServiceDidNotStartInTimeException some users are getting
        // dependencies.startSingleRunInner(RunSpecification.OnlyUploadMissingResults)
    }
    LaunchedEffect(Unit) {
        // Check for GeoIP DB updates in the background
        runCatching { dependencies.fetchGeoIpDbUpdates() }
    }
    LaunchedEffect(Unit) {
        dependencies.observeAndConfigureAutoUpdate()
    }
    LaunchedEffect(Unit) {
        dependencies.finishInProgressData()
        dependencies.deleteOldResults()
    }
    LaunchedEffect(Unit) {
        dependencies.observeAndConfigureAutoRun()
    }

    LaunchedEffect(deepLink) {
        when (deepLink) {
            is DeepLink.AddDescriptor -> {
                navController.navigate(
                    Screen.AddDescriptor(deepLink.id.toLongOrNull() ?: return@LaunchedEffect),
                )
                onDeeplinkHandled()
            }

            is DeepLink.RunUrls -> {
                navController.navigate(Screen.ChooseWebsites(deepLink.url))
                onDeeplinkHandled()
            }

            DeepLink.Error -> {
                snackbarHostState.showSnackbar(getString(Res.string.AddDescriptor_Toasts_Unsupported_Url))
                onDeeplinkHandled()
            }

            null -> Unit
        }
    }
}

private fun logAppStart(platformInfo: PlatformInfo) {
    with(platformInfo) {
        Logger.v(
            """
            ---APP START---
            Platform: $platform ($osVersion)"
            Version: $version
            Model: $model
            """.trimIndent(),
        )
    }
}

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState?> { null }

val MAIN_NAVIGATION_SCREENS = listOf(Screen.Dashboard, Screen.Results, Screen.Settings)
