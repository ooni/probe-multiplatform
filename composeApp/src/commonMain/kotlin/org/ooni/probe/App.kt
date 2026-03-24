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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ooniprobe.composeapp.generated.resources.AddDescriptor_Toasts_Unsupported_Url
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.getString
import org.ooni.probe.data.models.DeepLink
import org.ooni.probe.di.Dependencies
import org.ooni.probe.ui.navigation.BottomBarViewModel
import org.ooni.probe.ui.navigation.BottomNavigationBar
import org.ooni.probe.ui.navigation.Navigation
import org.ooni.probe.ui.navigation.Screen
import org.ooni.probe.ui.shared.ClipboardActions
import org.ooni.probe.ui.shared.LocalClipboardActions
import org.ooni.probe.ui.theme.AppTheme

@Composable
fun App(
    dependencies: Dependencies,
    deepLink: DeepLink?,
    onDeeplinkHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current

    val currentNavEntry by navController.currentBackStackEntryAsState()
    val isMainScreen = MAIN_NAVIGATION_SCREENS.any {
        currentNavEntry?.destination?.hasRoute(it::class) == true
    }

    CompositionLocalProvider(
        values = listOfNotNull(
            LocalSnackbarHostState provides snackbarHostState,
            LocalClipboardActions provides ClipboardActions(snackbarHostState, clipboard),
            dependencies.localeDirection?.invoke()?.let { LocalLayoutDirection provides it },
        ).toTypedArray(),
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

    // On App Open

    LaunchedEffect(Unit) {
        dependencies.observeAndConfigureAutoRun()
    }
    LaunchedEffect(Unit) {
        dependencies.observeAndConfigureAutoUpdate()
    }
    LaunchedEffect(Unit) {
        dependencies.refreshArticles()
    }

    LaunchedEffect(deepLink) {
        when (deepLink) {
            is DeepLink.AddDescriptor -> {
                navController.navigate(Screen.AddDescriptor(deepLink.id))
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

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState?> { null }

val MAIN_NAVIGATION_SCREENS: List<Screen> =
    listOf(Screen.Dashboard, Screen.Descriptors, Screen.Results, Screen.Settings)
