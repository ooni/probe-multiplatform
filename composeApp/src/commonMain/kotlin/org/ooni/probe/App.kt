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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.di.Dependencies
import org.ooni.probe.ui.navigation.BottomNavigationBar
import org.ooni.probe.ui.navigation.Navigation
import org.ooni.probe.ui.theme.AppTheme

@Composable
@Preview
fun App(dependencies: Dependencies) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    CompositionLocalProvider(
        values = arrayOf(LocalSnackbarHostState provides snackbarHostState),
    ) {
        AppTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = { BottomNavigationBar(navController) },
                ) { paddingValues ->
                    Box(
                        modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
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
        dependencies.bootstrapTestDescriptors()
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
