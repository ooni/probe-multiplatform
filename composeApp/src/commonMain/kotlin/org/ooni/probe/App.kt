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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.di.Dependencies
import org.ooni.probe.ui.navigation.BottomNavigationBar
import org.ooni.probe.ui.navigation.Navigation
import org.ooni.probe.ui.navigation.Screen
import org.ooni.probe.ui.theme.AppTheme

@Composable
@Preview
fun App(dependencies: Dependencies) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentNavEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavEntry?.destination?.route
    val isMainScreen = MAIN_NAVIGATION_SCREENS.map { it.route }.contains(currentRoute)

    CompositionLocalProvider(
        values = arrayOf(LocalSnackbarHostState provides snackbarHostState),
    ) {
        AppTheme(
            currentRoute = currentRoute,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (isMainScreen) {
                            BottomNavigationBar(navController)
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
        logAppStart(dependencies)
    }
    LaunchedEffect(Unit) {
        dependencies.bootstrapTestDescriptors()
    }
    LaunchedEffect(Unit) {
        dependencies.observeAndConfigureAutoRun()
    }
}

private fun logAppStart(dependencies: Dependencies) {
    with(dependencies.platformInfo) {
        Logger.i(
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
